package no.fintlabs.webresourceserver.security

import com.ninjasquad.springmockk.MockkBean
import no.fintlabs.webresourceserver.UrlPaths.EXTERNAL_API
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import no.fintlabs.webresourceserver.testutils.SecurityTestUtils
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

//@SpringBootTest
@WebMvcTest(controllers = [ExternalApiTestController::class])
//@AutoConfigureMockMvc
@ActiveProfiles("internal-api")
@Import(SecurityConfiguration::class)
class ExternalApiDisabledTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var jwtDecoder: JwtDecoder

    @MockkBean
    lateinit var clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService

    private val externalApiUrl = "$EXTERNAL_API/dummy"
    private val jwtString = "jwtString"

    @Test
    @Disabled
    fun `given token with clientId that is authorized, the request should return unauthorized`() {
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, "clientId1234")
        SecurityTestUtils.clientIsAuthorized(clientAuthorizationRequestService, "clientId1234", "1")

        mockMvc.get(externalApiUrl) {
            header(HttpHeaders.AUTHORIZATION, "Bearer $jwtString")
        }
            .andExpect {
                status { isUnauthorized() }
            }
    }

}