package no.fintlabs.webresourceserver.security.user

import no.fintlabs.webresourceserver.security.properties.InternalApiSecurityProperties
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class UserJwtConverter(
    private val securityProperties: InternalApiSecurityProperties,
    private val userClaimFormattingService: UserClaimFormattingService,
) : Converter<Jwt, AbstractAuthenticationToken> {
    companion object {
        private val log = LoggerFactory.getLogger(UserJwtConverter::class.java)
        private const val ORGANIZATION_ID = "organizationid"
        private const val OBJECT_IDENTIFIER = "objectidentifier"
        private const val ROLES = "roles"
    }

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val organizationId = jwt.getClaimAsString(ORGANIZATION_ID)
        val objectIdentifier = jwt.getClaimAsString(OBJECT_IDENTIFIER)
        val roles = jwt.getClaimAsStringList(ROLES).orEmpty()
        val adminRole = securityProperties.adminRole

        log.debug("Extracted organization ID from JWT: {}", organizationId)
        log.debug("Extracted roles from JWT: {}", roles)
        log.debug("Extracted objectIdentifier from JWT: {}", objectIdentifier)

        val modifiedClaims =
            jwt.claims
                .mapValues { (_, value) ->
                    if (value is String) userClaimFormattingService.removeDoubleQuotesFromClaim(value) else value
                }.filterValues { it != null }
                .toMutableMap()

        val sourceApplicationIdsString =
            userClaimFormattingService.convertSourceApplicationIdsIntoString(
                objectIdentifier,
            )
        modifiedClaims["sourceApplicationIds"] = sourceApplicationIdsString

        val modifiedJwt =
            Jwt
                .withTokenValue(jwt.tokenValue)
                .headers { it.putAll(jwt.headers) }
                .claims { it.putAll(modifiedClaims) }
                .build()

        val authorities =
            mutableListOf<GrantedAuthority>().apply {
                if (!organizationId.isNullOrBlank() && roles.isNotEmpty()) {
                    if (adminRole.isNotBlank() && adminRole in roles) {
                        add(SimpleGrantedAuthority("ROLE_ADMIN"))
                    }
                    roles.forEach { role ->
                        val authority = "ORGID_${organizationId}_ROLE_$role"
                        log.debug("orgIdAndRoleGrantedAuthorityString: {}", authority)
                        add(SimpleGrantedAuthority(authority))
                    }
                }
            }

        return JwtAuthenticationToken(modifiedJwt, authorities)
    }
}
