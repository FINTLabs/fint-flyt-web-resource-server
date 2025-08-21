package no.fintlabs.webresourceserver.security

import com.ninjasquad.springmockk.MockkBean
import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.fintlabs.webresourceserver.UrlPaths.EXTERNAL_API
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import no.fintlabs.webresourceserver.testutils.SecurityTestUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("external-api")
class ExternalApiEnabledTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var jwtDecoder: JwtDecoder

    @MockkBean
    lateinit var clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService

    private val externalApiUrl = "$EXTERNAL_API/dummy"
    private val jwtString = "jwtString"

    @Test
    fun `no token returns 401 Unauthorized`() {
        mockMvc
            .get(externalApiUrl)
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `no token does not call client authorization service`() {
        mockMvc
            .get(externalApiUrl)
            .andExpect {
                status { isUnauthorized() }
            }

        verify { clientAuthorizationRequestService.getClientAuthorization(any())?.wasNot(Called) }
    }

    @Test
    fun `token without client id returns 403 Forbidden`() {
        SecurityTestUtils.tokenDoesNotContainClientId(jwtDecoder, jwtString)

        mockMvc
            .get(externalApiUrl) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $jwtString")
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `no client id does not call client authorization service`() {
        SecurityTestUtils.tokenDoesNotContainClientId(jwtDecoder, jwtString)

        mockMvc
            .get(externalApiUrl) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $jwtString")
            }.andExpect {
                status { isForbidden() }
            }

        verify { clientAuthorizationRequestService.getClientAuthorization(any())?.wasNot(Called) }
    }

    @Test
    fun `token with client id calls authorization service with that id`() {
        val clientId = "clientId123"
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, clientId)

        every { clientAuthorizationRequestService.getClientAuthorization(clientId) } returns null

        mockMvc
            .get(externalApiUrl) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $jwtString")
            }.andExpect {
                status { isForbidden() }
            }

        verify(exactly = 1) { clientAuthorizationRequestService.getClientAuthorization(clientId) }
    }

    @Test
    fun `token with client id not authorized returns 403 Forbidden`() {
        val clientId = "clientId123"
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, clientId)
        SecurityTestUtils.clientIsNotAuthorized(clientAuthorizationRequestService, clientId)

        mockMvc
            .get(externalApiUrl) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $jwtString")
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `token with authorized client id returns 200 OK`() {
        val clientId = "clientId123"
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, clientId)
        SecurityTestUtils.clientIsAuthorized(clientAuthorizationRequestService, clientId, "1")

        mockMvc
            .get(externalApiUrl) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $jwtString")
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `token with client id but empty authorization response returns 403 Forbidden`() {
        val clientId = "clientId123"
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, clientId)
        SecurityTestUtils.authorizationRequestReturnsEmpty(clientAuthorizationRequestService, clientId)

        mockMvc
            .get(externalApiUrl) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $jwtString")
            }.andExpect {
                status { isForbidden() }
            }
    }
}
