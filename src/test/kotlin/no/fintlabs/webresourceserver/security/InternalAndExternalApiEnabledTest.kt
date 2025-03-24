package no.fintlabs.webresourceserver.security

import com.ninjasquad.springmockk.MockkBean
import no.fintlabs.webresourceserver.UrlPaths.EXTERNAL_API
import no.fintlabs.webresourceserver.UrlPaths.INTERNAL_API
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import no.fintlabs.webresourceserver.testutils.SecurityTestUtils.clientIsAuthorized
import no.fintlabs.webresourceserver.testutils.SecurityTestUtils.tokenContainsClientId
import no.fintlabs.webresourceserver.testutils.SecurityTestUtils.tokenContainsOrgIdAndRoles
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("external-api", "internal-api")
class InternalAndExternalApiEnabledTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var jwtDecoder: JwtDecoder

    @MockkBean
    lateinit var clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService

    private val externalApiUrl = "$EXTERNAL_API/dummy"
    private val internalApiUrl = "$INTERNAL_API/dummy"
    private val jwtString = "jwtString"

    @Test
    fun givenTokenClientIdThatIsAuthorizedTheRequestShouldReturnOk() {
        val clientId = "clientId1234"
        tokenContainsClientId(jwtDecoder, jwtString, clientId)
        clientIsAuthorized(clientAuthorizationRequestService, clientId, "1")

        mockMvc.get(externalApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun givenTokenWithOrgIdAndRoleThatIsAuthorizedTheRequestShouldReturnOk() {
        tokenContainsOrgIdAndRoles(jwtDecoder, jwtString, "example.no", listOf("admin"))

        mockMvc.get(internalApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isOk() }
        }
    }

}