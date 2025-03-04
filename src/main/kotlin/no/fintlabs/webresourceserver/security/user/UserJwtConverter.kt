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
    private val userClaimFormattingService: UserClaimFormattingService
) : Converter<Jwt, AbstractAuthenticationToken> {

    private val log = LoggerFactory.getLogger(UserJwtConverter::class.java)

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val organizationId = jwt.getClaimAsString("organizationid")
        val objectIdentifier = jwt.getClaimAsString("objectidentifier")
        val roles = jwt.getClaimAsStringList("roles")
        val adminRole = securityProperties.adminRole

        log.debug("Extracted organization ID from JWT: {}", organizationId)
        log.debug("Extracted roles from JWT: {}", roles)
        log.debug("Extracted objectIdentifier from JWT: {}", objectIdentifier)

        val modifiedClaims = jwt.claims.entries
            .map { entry ->
                if (entry.value is String) {
                    entry.key to userClaimFormattingService.removeDoubleQuotesFromClaim(entry.value as String)
                } else {
                    entry.key to entry.value
                }
            }
            .filter { it.second != null }
            .toMap(mutableMapOf())

        val sourceApplicationIdsString = userClaimFormattingService.convertSourceApplicationIdsIntoString(objectIdentifier)
        modifiedClaims["sourceApplicationIds"] = sourceApplicationIdsString

        val modifiedJwt = Jwt.withTokenValue(jwt.tokenValue)
            .headers { it.putAll(jwt.headers) }
            .claims { it.putAll(jwt.claims) }
            .build()

        val authorities = mutableListOf<GrantedAuthority>()

        if (organizationId != null && roles != null) {
            if (adminRole.isNotBlank()) {
                val isAdmin = roles.contains(adminRole)
                if (isAdmin) {
                    authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
                }
            }
            for (role in roles) {
                val authority = "ORGID_${organizationId}_ROLE_$role"
                log.debug("orgIdAndRoleGrantedAuthorityString: {}", authority)
                authorities.add(SimpleGrantedAuthority(authority))
            }
        }

        return JwtAuthenticationToken(modifiedJwt, authorities)
    }
}