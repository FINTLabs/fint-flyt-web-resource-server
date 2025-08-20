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
    override fun convert(source: Jwt): AbstractAuthenticationToken {
        val sub = source.getClaim<String>("sub") ?: return JwtAuthenticationToken(source)

        val maybeAuthorization = sourceApplicationAuthorizationRequestService.getClientAuthorization(sub)

        val authority =
            maybeAuthorization
                .map { auth -> sourceApplicationAuthorizationService.getAuthority(auth) }
                .orElse(null)

        return if (authority != null) {
            JwtAuthenticationToken(source, listOf(authority))
        } else {
            JwtAuthenticationToken(source)
        }
    }
}
