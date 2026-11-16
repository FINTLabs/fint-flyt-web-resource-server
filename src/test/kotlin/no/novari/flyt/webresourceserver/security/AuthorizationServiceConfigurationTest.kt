package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClientProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.ServerSocket
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class AuthorizationServiceConfigurationTest {
    private val contextRunner =
        WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    OAuth2ClientAutoConfiguration::class.java,
                    HttpClientAutoConfiguration::class.java,
                    RestClientAutoConfiguration::class.java,
                    AuthorizationServiceConfiguration::class.java,
                ),
            ).withBean(AuthorityMappingService::class.java)

    @Test
    fun `token request to NAM times out after 5 seconds by default`() {
        // The socket accepts the connection but never answers, like a hanging NAM.
        ServerSocket(0).use { nam ->
            contextRunner
                .withPropertyValues(*registration("http://localhost:${nam.localPort}/token"))
                .run { context ->
                    val elapsed = context.timeToFailure(context.authorizationServiceRestClient())

                    assertThat(elapsed).isBetween(Duration.ofSeconds(4), Duration.ofSeconds(10))
                }
        }
    }

    @Test
    fun `spring http client read timeout overrides the default`() {
        ServerSocket(0).use { authorizationService ->
            contextRunner
                .withPropertyValues(*registration("http://localhost/token"), "spring.http.client.read-timeout=200ms")
                .run { context ->
                    // Without the OAuth2 interceptor, so only the call to authorization-service is timed.
                    val client =
                        context
                            .authorizationServiceRestClient()
                            .mutate()
                            .requestInterceptors { it.clear() }
                            .baseUrl("http://localhost:${authorizationService.localPort}")
                            .build()

                    assertThat(context.timeToFailure(client)).isLessThan(Duration.ofSeconds(2))
                }
        }
    }

    @Test
    fun `callers share one token`() {
        val registration =
            ClientRegistration
                .withRegistrationId("authorization-service")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientId("client")
                .tokenUri("http://idp/token")
                .build()
        val registrations = InMemoryClientRegistrationRepository(registration)
        val tokenRequests = AtomicInteger()
        val manager =
            AuthorizedClientServiceOAuth2AuthorizedClientManager(
                registrations,
                InMemoryOAuth2AuthorizedClientService(registrations),
            )
        manager.setAuthorizedClientProvider(
            OAuth2AuthorizedClientProvider { context ->
                context.authorizedClient ?: OAuth2AuthorizedClient(
                    context.clientRegistration,
                    context.principal.name,
                    OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token-${tokenRequests.incrementAndGet()}",
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                    ),
                )
            },
        )
        val builder =
            AuthorizationServiceConfiguration()
                .authorizationServiceRestClient(
                    manager,
                    RestClient.builder(),
                    ClientHttpRequestFactoryBuilder.detect(),
                    ClientHttpRequestFactorySettings.defaults(),
                    UserAuthorizationClientProperties(),
                ).mutate()
        val server = MockRestServiceServer.bindTo(builder).build()
        server
            .expect(ExpectedCount.twice(), { })
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1"))
            .andRespond(withSuccess())
        val client = builder.build()

        try {
            listOf("user-a", "user-b").forEach { caller ->
                SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(caller, null)
                client
                    .get()
                    .uri("/x")
                    .retrieve()
                    .toBodilessEntity()
            }
        } finally {
            SecurityContextHolder.clearContext()
        }

        server.verify()
        assertThat(tokenRequests).hasValue(1)
    }

    private fun registration(tokenUri: String) =
        arrayOf(
            "spring.security.oauth2.client.provider.fint-idp.token-uri=$tokenUri",
            "spring.security.oauth2.client.registration.authorization-service.provider=fint-idp",
            "spring.security.oauth2.client.registration.authorization-service.authorization-grant-type=" +
                "client_credentials",
            "spring.security.oauth2.client.registration.authorization-service.client-id=client",
            "spring.security.oauth2.client.registration.authorization-service.client-secret=secret",
        )

    private fun AssertableWebApplicationContext.authorizationServiceRestClient() =
        getBean("authorizationServiceRestClient", RestClient::class.java)

    private fun AssertableWebApplicationContext.timeToFailure(client: RestClient): Duration {
        assertThat(this).hasNotFailed()
        val start = System.nanoTime()
        assertThrows<RuntimeException> {
            client
                .get()
                .uri("/x")
                .retrieve()
                .toBodilessEntity()
        }
        return Duration.ofNanos(System.nanoTime() - start)
    }
}
