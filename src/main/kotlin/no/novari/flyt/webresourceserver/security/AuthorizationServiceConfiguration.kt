package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.flyt.webresourceserver.security.user.authorization.RestClientUserAuthorizationClient
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClient
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClientProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.web.client.RestClient
import java.time.Duration

@AutoConfiguration
@AutoConfigureAfter(OAuth2ClientAutoConfiguration::class)
@ConditionalOnClass(ClientRegistrationRepository::class)
@EnableConfigurationProperties(UserAuthorizationClientProperties::class)
class AuthorizationServiceConfiguration {
    @Bean
    fun sourceApplicationAuthorizationService(
        authorityMappingService: AuthorityMappingService,
    ): SourceApplicationAuthorizationService {
        return SourceApplicationAuthorizationService(authorityMappingService)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.security.oauth2.client.registration.authorization-service",
        name = ["client-id"],
    )
    fun userAuthorizationService(
        authorityMappingService: AuthorityMappingService,
        userAuthorizationClient: UserAuthorizationClient,
    ): UserAuthorizationService = UserAuthorizationService(authorityMappingService, userAuthorizationClient)

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.security.oauth2.client.registration.authorization-service",
        name = ["client-id"],
    )
    fun authorizationServiceAuthorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
        requestFactoryBuilder: ClientHttpRequestFactoryBuilder<*>,
        requestFactorySettings: ClientHttpRequestFactorySettings,
    ): OAuth2AuthorizedClientManager {
        // Same converters and error handler as the token client's default RestClient, plus our timeouts.
        val tokenRestClient =
            RestClient
                .builder()
                .requestFactory(requestFactoryBuilder.build(requestFactorySettings.withDefaultTimeouts()))
                .messageConverters { converters ->
                    converters.clear()
                    converters.add(FormHttpMessageConverter())
                    converters.add(OAuth2AccessTokenResponseHttpMessageConverter())
                }.defaultStatusHandler(OAuth2ErrorResponseErrorHandler())
                .build()
        val tokenResponseClient = RestClientClientCredentialsTokenResponseClient()
        tokenResponseClient.setRestClient(tokenRestClient)

        val manager =
            AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService,
            )
        manager.setAuthorizedClientProvider(
            OAuth2AuthorizedClientProviderBuilder
                .builder()
                .clientCredentials { it.accessTokenResponseClient(tokenResponseClient) }
                .build(),
        )
        return manager
    }

    // Shared by user and source application authorization.
    @Bean
    @ConditionalOnProperty(
        prefix = "spring.security.oauth2.client.registration.authorization-service",
        name = ["client-id"],
    )
    fun authorizationServiceRestClient(
        @Qualifier("authorizationServiceAuthorizedClientManager") authorizedClientManager:
            OAuth2AuthorizedClientManager,
        restClientBuilder: RestClient.Builder,
        requestFactoryBuilder: ClientHttpRequestFactoryBuilder<*>,
        requestFactorySettings: ClientHttpRequestFactorySettings,
        properties: UserAuthorizationClientProperties,
    ): RestClient {
        val interceptor = OAuth2ClientHttpRequestInterceptor(authorizedClientManager)
        interceptor.setClientRegistrationIdResolver { properties.clientRegistrationId }

        return restClientBuilder
            .requestFactory(requestFactoryBuilder.build(requestFactorySettings.withDefaultTimeouts()))
            .requestInterceptor(interceptor)
            .baseUrl("${properties.baseUrl}/api/intern-klient/authorization")
            .build()
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.security.oauth2.client.registration.authorization-service",
        name = ["client-id"],
    )
    fun userAuthorizationClient(
        @Qualifier("authorizationServiceRestClient") restClient: RestClient,
    ): UserAuthorizationClient = RestClientUserAuthorizationClient(restClient)

    // The calls block incoming requests, so they must not fall back to the HTTP client's minutes-long defaults.
    // spring.http.client.connect-timeout and read-timeout in the host app override these.
    private fun ClientHttpRequestFactorySettings.withDefaultTimeouts(): ClientHttpRequestFactorySettings =
        withTimeouts(connectTimeout() ?: DEFAULT_CONNECT_TIMEOUT, readTimeout() ?: DEFAULT_READ_TIMEOUT)

    private companion object {
        val DEFAULT_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(2)
        val DEFAULT_READ_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}
