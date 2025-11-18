package no.novari.flyt.resourceserver.security.client.internal

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class InternalClientJwtConverter(
    private val internalClientAuthorityMappingService: InternalClientAuthorityMappingService,
) : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(source: Jwt): AbstractAuthenticationToken {
        val subject =
            source.subject ?: throw BadCredentialsException("Missing subject for internal client authentication")
        val authority = internalClientAuthorityMappingService.createInternalClientIdAuthority(subject)
        return JwtAuthenticationToken(source, listOf(authority))
    }
}
