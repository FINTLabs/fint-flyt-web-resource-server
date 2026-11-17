package no.novari.flyt.webresourceserver.security.client.sourceapplication

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

public class SourceApplicationJwtConverter internal constructor(
    private val sourceApplicationAuthorizationClient: SourceApplicationAuthorizationClient,
    private val sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService,
) : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(source: Jwt): AbstractAuthenticationToken {
        val subject =
            source.subject
                ?: throw BadCredentialsException("Missing subject for source application authentication")

        val authorization =
            sourceApplicationAuthorizationClient.getClientAuthorization(subject)
                ?: throw BadCredentialsException("Client authorization not found for subject: $subject")

        val sourceApplicationId =
            authorization
                .takeIf { it.authorized }
                ?.sourceApplicationId
                ?: throw BadCredentialsException("Client is not authorized for any source application")

        val authority = sourceApplicationAuthorityMappingService.createSourceApplicationAuthority(sourceApplicationId)
        return JwtAuthenticationToken(source, listOf(authority))
    }
}
