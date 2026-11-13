package no.novari.flyt.webresourceserver.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.web.client.RestClient
import java.net.ServerSocket
import java.time.Duration

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
