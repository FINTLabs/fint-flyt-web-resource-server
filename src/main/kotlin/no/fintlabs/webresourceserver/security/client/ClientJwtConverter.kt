package no.fintlabs.webresourceserver.security.client

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service

@Service
class ClientJwtConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(source: Jwt): AbstractAuthenticationToken {
        val sub = source.getClaim("sub") as? String
        return if (sub != null) {
            val authority = ClientAuthorizationUtil.getAuthority(sub)
            JwtAuthenticationToken(source, listOf(authority))
        } else {
            JwtAuthenticationToken(source)
        }
    }
}
