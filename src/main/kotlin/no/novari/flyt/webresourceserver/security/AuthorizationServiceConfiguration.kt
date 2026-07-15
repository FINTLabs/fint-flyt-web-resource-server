package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.flyt.webresourceserver.security.user.authorization.CachingUserAuthorizationClient
import no.novari.flyt.webresourceserver.security.user.authorization.RestClientUserAuthorizationClient
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClient
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClientProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient

@AutoConfiguration
@AutoConfigureAfter(OAuth2ClientAutoConfiguration::class)
@EnableConfigurationProperties(UserAuthorizationClientProperties::class)
class AuthorizationServiceConfiguration {
    @Bean
    fun sourceApplicationAuthorizationService(
        authorityMappingService: AuthorityMappingService,
    ): SourceApplicationAuthorizationService {
        return SourceApplicationAuthorizationService(authorityMappingService)
    }

    @Bean
    @ConditionalOnBean(UserAuthorizationClient::class)
    fun userAuthorizationService(
        authorityMappingService: AuthorityMappingService,
        userAuthorizationClient: UserAuthorizationClient,
    ): UserAuthorizationService = UserAuthorizationService(authorityMappingService, userAuthorizationClient)

    @Bean
    @ConditionalOnBean(
        ClientRegistrationRepository::class,
        OAuth2AuthorizedClientService::class,
    )
    fun userAuthorizationAuthorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
    ): OAuth2AuthorizedClientManager {
        val manager =
            AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService,
            )
        manager.setAuthorizedClientProvider(
            OAuth2AuthorizedClientProviderBuilder
                .builder()
                .clientCredentials()
                .build(),
        )
        return manager
    }

    @Bean("userAuthorizationRestClient")
    @ConditionalOnBean(name = ["userAuthorizationAuthorizedClientManager"])
    fun userAuthorizationRestClient(
        userAuthorizationAuthorizedClientManager: OAuth2AuthorizedClientManager,
        clientHttpRequestFactory: ObjectProvider<ClientHttpRequestFactory>,
        restClientBuilder: RestClient.Builder,
        properties: UserAuthorizationClientProperties,
    ): RestClient {
        val interceptor = OAuth2ClientHttpRequestInterceptor(userAuthorizationAuthorizedClientManager)
        interceptor.setClientRegistrationIdResolver { properties.clientRegistrationId }

        val builder =
            restClientBuilder
                .requestInterceptor(interceptor)
                .baseUrl("${properties.baseUrl}/api/intern-klient/authorization/users")
        clientHttpRequestFactory.ifAvailable { builder.requestFactory(it) }
        return builder.build()
    }

    @Bean
    @ConditionalOnBean(name = ["userAuthorizationRestClient"])
    fun userAuthorizationClient(
        @Qualifier("userAuthorizationRestClient") restClient: RestClient,
        properties: UserAuthorizationClientProperties,
    ): UserAuthorizationClient =
        CachingUserAuthorizationClient(
            RestClientUserAuthorizationClient(restClient),
            properties.cache,
        )
}
