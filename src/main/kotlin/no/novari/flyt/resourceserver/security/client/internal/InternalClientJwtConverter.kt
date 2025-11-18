package no.novari.flyt.resourceserver.security.client.internal

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono

class InternalClientJwtConverter(
    private val internalClientAuthorityMappingService: InternalClientAuthorityMappingService
) : Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    override fun convert(source: Jwt): Mono<AbstractAuthenticationToken> = Mono.fromCallable {
        source.subject
            ?.let(internalClientAuthorityMappingService::createInternalClientIdAuthority)
            ?.let { authority -> JwtAuthenticationToken(source, listOf(authority)) }
            ?: JwtAuthenticationToken(source)
    }
}
