package no.fintlabs.webresourceserver.security

import com.ninjasquad.springmockk.MockkBean
import no.fintlabs.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("internal-api")
class InternalClientApiDisabledTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var jwtDecoder: JwtDecoder

    @MockkBean
    lateinit var clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService

    private val internalClientApiUrl = "$INTERNAL_CLIENT_API/dummy"
    private val jwtString = "jwtString"

    @Test
    fun givenTokenWithClientIdTheRequestShouldReturnForbidden() {
        SecurityTestUtils.tokenContainsClientId(jwtDecoder, jwtString, "1234")

        mockMvc.get(internalClientApiUrl) {
            header("Authorization", "Bearer $jwtString")
        }.andExpect {
            status { isForbidden() }
        }
    }

}