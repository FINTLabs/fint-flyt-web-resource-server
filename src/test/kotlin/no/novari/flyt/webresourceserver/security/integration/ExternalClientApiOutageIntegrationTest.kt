package no.novari.flyt.webresourceserver.security.integration

import no.novari.flyt.webresourceserver.UrlPaths
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationClient
import no.novari.flyt.webresourceserver.security.integration.parameters.TokenFactory
import no.novari.flyt.webresourceserver.security.integration.values.ClientId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.HttpServerErrorException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("external-client-api")
class ExternalClientApiOutageIntegrationTest {
    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @MockitoBean
    private lateinit var sourceApplicationAuthorizationClient: SourceApplicationAuthorizationClient

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    // Runs through the real servlet container, so the error dispatch decides the final status.
    @Test
    fun `authorization-service outage gives 401`() {
        val clientId = ClientId.WITH_EXTERNAL_CLIENT_SA_AUTHORIZATION_ID_1.claimValue
        val token = TokenFactory.createClientToken(ClientId.WITH_EXTERNAL_CLIENT_SA_AUTHORIZATION_ID_1).token!!
        `when`(jwtDecoder.decode(token.tokenValue)).thenReturn(token)
        `when`(sourceApplicationAuthorizationClient.getClientAuthorization(clientId))
            .thenThrow(HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))

        val headers = HttpHeaders().apply { setBearerAuth(token.tokenValue) }
        val response =
            testRestTemplate.exchange<String>(
                "${UrlPaths.EXTERNAL_API}/dummy",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
