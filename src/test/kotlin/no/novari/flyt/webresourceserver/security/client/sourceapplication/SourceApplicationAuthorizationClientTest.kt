package no.novari.flyt.webresourceserver.security.client.sourceapplication

import no.novari.flyt.webresourceserver.security.AuthorizationServiceConfiguration
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClientProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.ExpectedCount.twice
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.time.Instant

class SourceApplicationAuthorizationClientTest {
    private val url = "http://fint-flyt-authorization-service:8080/api/intern-klient/authorization/clients/client-1"
    private val restClientBuilder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val client =
        SourceApplicationAuthorizationClient(
            AuthorizationServiceConfiguration().authorizationServiceRestClient(
                authorizedClientManager(),
                restClientBuilder,
                UserAuthorizationClientProperties(),
            ),
        )

    @AfterEach
    fun verify() {
        server.verify()
    }

    @Test
    fun `sends bearer token and caches the authorization`() {
        server
            .expect(once(), requestTo(url))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
            .andRespond(
                withSuccess(
                    """{"authorized":true,"clientId":"client-1","sourceApplicationId":3}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val expected = SourceApplicationAuthorization(authorized = true, clientId = "client-1", sourceApplicationId = 3)
        assertThat(client.getClientAuthorization("client-1")).isEqualTo(expected)
        assertThat(client.getClientAuthorization("client-1")).isEqualTo(expected)
    }

    @Test
    fun `404 returns null and is not cached`() {
        server.expect(twice(), requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertThat(client.getClientAuthorization("client-1")).isNull()
        assertThat(client.getClientAuthorization("client-1")).isNull()
    }

    @Test
    fun `server error is thrown and not cached`() {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))
        server
            .expect(requestTo(url))
            .andRespond(withSuccess("""{"authorized":true,"sourceApplicationId":3}""", MediaType.APPLICATION_JSON))

        assertThrows<HttpServerErrorException> { client.getClientAuthorization("client-1") }
        assertThat(client.getClientAuthorization("client-1")?.sourceApplicationId).isEqualTo(3L)
    }

    private fun authorizedClientManager(): OAuth2AuthorizedClientManager {
        val registration =
            ClientRegistration
                .withRegistrationId("authorization-service")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientId("client")
                .tokenUri("http://idp/token")
                .build()
        val token =
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
            )
        val manager = mock(OAuth2AuthorizedClientManager::class.java)
        `when`(manager.authorize(any())).thenReturn(OAuth2AuthorizedClient(registration, "anonymousUser", token))
        return manager
    }
}
