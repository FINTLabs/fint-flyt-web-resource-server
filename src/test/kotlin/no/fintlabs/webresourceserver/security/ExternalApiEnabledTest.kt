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
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.*

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
    fun givenNoTokenShouldReturnUnauthorized() {
        mockMvc.get(externalApiUrl)
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun givenNoTokenShouldNotCallClientAuthorizationRequestService() {
        mockMvc.get(externalApiUrl)
            .andExpect {
                status { isUnauthorized() }
            }

        verify { clientAuthorizationRequestService.getClientAuthorization(any()) wasNot Called }
    }

    @Test
    fun givenTokenWithoutClientIdShouldReturnForbidden() {
        SecurityTestUtils.tokenDoesNotContainClientId(jwtDecoder, jwtString)

        mockMvc.get(externalApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun givenNoClientIdShouldNotCallClientAuthorizationRequestService() {
        SecurityTestUtils.tokenDoesNotContainClientId(jwtDecoder, jwtString)

        mockMvc.get(externalApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isForbidden() }
        }

        verify { clientAuthorizationRequestService.getClientAuthorization(any()) wasNot Called }
    }

    @Test
    fun givenTokenWithClientIdShouldCallClientAuthorizationRequestServiceWithClientId() {
        val clientId = "clientId123"
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, clientId)

        every { clientAuthorizationRequestService.getClientAuthorization(clientId) } returns Optional.empty()

        mockMvc.get(externalApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isForbidden() }
        }

        verify(exactly = 1) { clientAuthorizationRequestService.getClientAuthorization(clientId) }
    }

    @Test
    fun givenTokenWithClientIdThatIsNotAuthorizedShouldReturnForbidden() {
        val clientId = "clientId123"
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, clientId)
        SecurityTestUtils.clientIsNotAuthorized(clientAuthorizationRequestService, clientId)

        mockMvc.get(externalApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun givenTokenWithClientIdAndAuthorizedShouldReturnOk() {
        val clientId = "clientId123"
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, clientId)
        SecurityTestUtils.clientIsAuthorized(clientAuthorizationRequestService, clientId, "1")

        mockMvc.get(externalApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun givenTokenWithClientIdButNoEmptyResponseFromInternalAuthorizationShouldReturnForbidden() {
        val clientId = "clientId123"
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, clientId)
        SecurityTestUtils.authorizationRequestReturnsEmpty(clientAuthorizationRequestService, clientId)

        mockMvc.get(externalApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isForbidden() }
        }
    }
}
