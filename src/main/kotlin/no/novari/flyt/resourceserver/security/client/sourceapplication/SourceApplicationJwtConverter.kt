package no.novari.flyt.resourceserver.security.client.sourceapplication

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SourceApplicationJwtConverter(
    private val sourceApplicationAuthorizationRequestService: SourceApplicationAuthorizationRequestService,
    private val sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService
) : Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    override fun convert(source: Jwt): Mono<AbstractAuthenticationToken> = Mono.fromCallable {
        source.subject
            ?.let(sourceApplicationAuthorizationRequestService::getClientAuthorization)
            ?.orElse(null)
            ?.sourceApplicationId
            ?.let(sourceApplicationAuthorityMappingService::createSourceApplicationAuthority)
            ?.let { authority -> JwtAuthenticationToken(source, listOf(authority)) }
            ?: JwtAuthenticationToken(source)
    }
}
