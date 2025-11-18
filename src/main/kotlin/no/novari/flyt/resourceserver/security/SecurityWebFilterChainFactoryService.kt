package no.novari.flyt.resourceserver.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authorization.ReactiveAuthorizationManager
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authorization.AuthorizationContext
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SecurityWebFilterChainFactoryService {

    fun createFilterChain(
        http: ServerHttpSecurity,
        path: String,
        converter: Converter<Jwt, Mono<AbstractAuthenticationToken>>,
        manager: ReactiveAuthorizationManager<AuthorizationContext>
    ): SecurityWebFilterChain = addCommonConfig(http)
        .securityMatcher(PathPatternParserServerWebExchangeMatcher("$path/**"))
        .oauth2ResourceServer { resourceServer ->
            resourceServer.jwt { jwtSpec ->
                jwtSpec.jwtAuthenticationConverter(converter)
            }
        }
        .authorizeExchange { exchange -> exchange.anyExchange().access(manager) }
        .build()

    fun permitAll(http: ServerHttpSecurity, path: String): SecurityWebFilterChain = addCommonConfig(http)
        .securityMatcher(PathPatternParserServerWebExchangeMatcher("$path/**"))
        .authorizeExchange { spec -> spec.anyExchange().permitAll() }
        .build()

    fun denyAll(http: ServerHttpSecurity, path: String): SecurityWebFilterChain =
        denyAll(http.securityMatcher(PathPatternParserServerWebExchangeMatcher("$path/**")))

    fun denyAll(http: ServerHttpSecurity): SecurityWebFilterChain = addCommonConfig(http)
        .authorizeExchange { exchange -> exchange.anyExchange().denyAll() }
        .build()

    private fun addCommonConfig(http: ServerHttpSecurity): ServerHttpSecurity = http
        .addFilterBefore(AuthorizationLogFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
}
