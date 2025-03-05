package no.fintlabs.webresourceserver.security

import com.ninjasquad.springmockk.MockkBean
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.verify
import no.fintlabs.webresourceserver.UrlPaths.EXTERNAL_API
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [ExternalApiTestController::class])
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

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `given no token should not call clientAuthorizationRequestService`() {
        mockMvc.get(externalApiUrl)
            .andExpect {
                status { isUnauthorized() }
            }

        verify { clientAuthorizationRequestService.getClientAuthorization(any()) wasNot Called }
    }

}