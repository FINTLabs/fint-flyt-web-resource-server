package no.fintlabs.webresourceserver.security.client.sourceapplication

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service

@Service
class SourceApplicationJwtConverter(
    private val sourceApplicationAuthorizationService: SourceApplicationAuthorizationService,
    private val sourceApplicationAuthorizationRequestService: SourceApplicationAuthorizationRequestService,
) : Converter<Jwt, AbstractAuthenticationToken> {
    companion object {
        private const val CLAIM_SUB = "sub"
    }

    override fun convert(source: Jwt): AbstractAuthenticationToken {
        val sub = source.getClaimAsString(CLAIM_SUB) ?: return JwtAuthenticationToken(source)

        val maybeAuthorization = sourceApplicationAuthorizationRequestService.getClientAuthorization(sub)

        val authority =
            maybeAuthorization?.let { auth ->
                sourceApplicationAuthorizationService.getAuthority(auth)
            }

        return authority?.let { JwtAuthenticationToken(source, listOf(it)) } ?: JwtAuthenticationToken(source)
    }
}
