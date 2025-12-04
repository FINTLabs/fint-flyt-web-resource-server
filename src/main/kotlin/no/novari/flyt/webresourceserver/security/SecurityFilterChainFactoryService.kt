package no.novari.flyt.webresourceserver.security

import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.HttpStatusAccessDeniedHandler
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.stereotype.Service
import org.springframework.web.util.pattern.PathPatternParser

@Service
class SecurityFilterChainFactoryService {
    private val pathPatternParser = PathPatternParser()

    fun createFilterChain(
        http: HttpSecurity,
        path: String,
        converter: Converter<Jwt, out AbstractAuthenticationToken>,
        manager: AuthorizationManager<RequestAuthorizationContext>,
    ): SecurityFilterChain {
        return addCommonConfig(http)
            .securityMatcher(matcher(path))
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(converter)
                }
            }.authorizeHttpRequests { requests ->
                requests.anyRequest().access(manager)
            }.build()
    }

    fun permitAll(
        http: HttpSecurity,
        path: String,
    ): SecurityFilterChain {
        return addCommonConfig(http)
            .securityMatcher(matcher(path))
            .authorizeHttpRequests { requests ->
                requests.anyRequest().permitAll()
            }.build()
    }

    fun denyAll(
        http: HttpSecurity,
        path: String,
    ): SecurityFilterChain {
        return denyAll(http.securityMatcher(matcher(path)))
    }

    fun denyAll(http: HttpSecurity): SecurityFilterChain {
        return addCommonConfig(http)
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                exceptions.accessDeniedHandler(HttpStatusAccessDeniedHandler(HttpStatus.UNAUTHORIZED))
            }.authorizeHttpRequests { requests -> requests.anyRequest().denyAll() }
            .build()
    }

    private fun addCommonConfig(http: HttpSecurity): HttpSecurity {
        return http
            .addFilterBefore(AuthorizationLogFilter(), BearerTokenAuthenticationFilter::class.java)
            .csrf { it.disable() }
            .httpBasic { it.disable() }
    }

    private fun matcher(path: String): PathPatternRequestMatcher {
        return PathPatternRequestMatcher.withPathPatternParser(pathPatternParser).matcher("$path/**")
    }
}
