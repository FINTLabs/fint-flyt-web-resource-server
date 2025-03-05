package no.fintlabs.webresourceserver.security

import no.fintlabs.cache.FintCacheConfiguration
import no.fintlabs.webresourceserver.UrlPaths
import no.fintlabs.webresourceserver.security.client.ClientJwtConverter
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationJwtConverter
import no.fintlabs.webresourceserver.security.properties.ApiSecurityProperties
import no.fintlabs.webresourceserver.security.properties.ExternalApiSecurityProperties
import no.fintlabs.webresourceserver.security.properties.InternalApiSecurityProperties
import no.fintlabs.webresourceserver.security.properties.InternalClientApiSecurityProperties
import no.fintlabs.webresourceserver.security.user.UserClaimFormattingService
import no.fintlabs.webresourceserver.security.user.UserJwtConverter
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
@EnableAutoConfiguration
@Import(FintCacheConfiguration::class)
class SecurityConfiguration(
    private val userClaimFormattingService: UserClaimFormattingService,
) {

    private val log = LoggerFactory.getLogger(SecurityConfiguration::class.java)

    @Bean
    @ConfigurationProperties("fint.flyt.webresourceserver.security.api.internal")
    fun internalApiSecurityProperties(): InternalApiSecurityProperties = InternalApiSecurityProperties()

    @Bean
    @ConfigurationProperties("fint.flyt.webresourceserver.security.api.internal-client")
    fun internalClientApiSecurityProperties(): InternalClientApiSecurityProperties =
        InternalClientApiSecurityProperties()

    @Bean
    @ConfigurationProperties("fint.flyt.webresourceserver.security.api.external")
    fun externalApiSecurityProperties(): ExternalApiSecurityProperties = ExternalApiSecurityProperties()

    @Order(1)
    @Bean
    fun internalApiFilterChain(
        http: HttpSecurity,
        internalApiSecurityProperties: InternalApiSecurityProperties
    ): SecurityFilterChain {
        log.debug(
            "Internal API Security Properties: {}",
            internalApiSecurityProperties.getPermittedAuthorities()
        )
        return createFilterChain(
            http,
            "${UrlPaths.INTERNAL_API}/**",
            UserJwtConverter(internalApiSecurityProperties, userClaimFormattingService),
            internalApiSecurityProperties
        )
    }

    @Order(2)
    @Bean
    fun internalClientApiFilterChain(
        http: HttpSecurity,
        internalClientApiSecurityProperties: InternalClientApiSecurityProperties,
        clientJwtConverter: ClientJwtConverter
    ): SecurityFilterChain {
        return createFilterChain(
            http,
            "${UrlPaths.INTERNAL_CLIENT_API}/**",
            clientJwtConverter,
            internalClientApiSecurityProperties
        )
    }

    @Order(3)
    @Bean
    fun externalApiFilterChain(
        http: HttpSecurity,
        externalApiSecurityProperties: ExternalApiSecurityProperties,
        sourceApplicationJwtConverter: SourceApplicationJwtConverter
    ): SecurityFilterChain {
        return createFilterChain(
            http,
            "${UrlPaths.EXTERNAL_API}/**",
            sourceApplicationJwtConverter,
            externalApiSecurityProperties
        )
    }

    @Order(4)
    @Bean
    fun globalFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.addFilterBefore(AuthorizationLogFilter(), BearerTokenAuthenticationFilter::class.java)
        return denyAll(http)
    }

    private fun createFilterChain(
        http: HttpSecurity,
        path: String,
        converter: Converter<Jwt, AbstractAuthenticationToken>,
        apiSecurityProperties: ApiSecurityProperties,
    ): SecurityFilterChain {
        http.securityMatcher(AntPathRequestMatcher(path))
            .addFilterBefore(AuthorizationLogFilter(), BearerTokenAuthenticationFilter::class.java)

        if (!apiSecurityProperties.enabled) {
            return denyAll(http)
        }

        return if (apiSecurityProperties.permitAll) {
            permitAll(http)
        } else {
            http.oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(converter)
                }
            }

            http.authorizeHttpRequests { requests ->
                requests.anyRequest()
                    .hasAnyAuthority(*apiSecurityProperties.getPermittedAuthorities())
            }
            http.build()
        }
    }

    private fun permitAll(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests { requests ->
            requests.anyRequest().permitAll()
        }
        return http.build()
    }

    private fun denyAll(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests { requests ->
            requests.anyRequest().denyAll()
        }
        return http.build()
    }

}