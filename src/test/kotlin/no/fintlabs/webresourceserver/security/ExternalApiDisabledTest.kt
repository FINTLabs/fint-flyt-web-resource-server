package no.fintlabs.webresourceserver.security

import com.ninjasquad.springmockk.MockkBean
import no.fintlabs.webresourceserver.UrlPaths.EXTERNAL_API
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import no.fintlabs.webresourceserver.testutils.SecurityTestUtils.clientIsAuthorized
import no.fintlabs.webresourceserver.testutils.SecurityTestUtils.tokenContainsClientId
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
@ActiveProfiles("internal-api")
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
    fun givenTokenWithClientIdThatIsAuthorizedTheRequestShouldReturnForbidden() {
        tokenContainsClientId(jwtDecoder, jwtString, "clientId1234")
        clientIsAuthorized(clientAuthorizationRequestService, "clientId1234", "1")

        mockMvc
            .get(externalApiUrl) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $jwtString")
            }.andExpect {
                status { isForbidden() }
            }
    }
}
