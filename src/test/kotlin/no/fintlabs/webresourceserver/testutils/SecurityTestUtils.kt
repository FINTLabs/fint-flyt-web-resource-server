package no.fintlabs.webresourceserver.testutils

import io.mockk.every
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorization
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.*

object SecurityTestUtils {

    fun tokenContainsClientId(
        jwtDecoder: org.springframework.security.oauth2.jwt.JwtDecoder,
        jwtString: String,
        clientId: String
    ) {
        every { jwtDecoder.decode(jwtString) } returns Jwt(
            jwtString,
            Instant.now(),
            Instant.now().plusSeconds(20),
            mapOf("header1" to "header1"),
            mapOf("sub" to clientId)
        )
    }

    fun tokenDoesNotContainClientId(
        jwtDecoder: org.springframework.security.oauth2.jwt.JwtDecoder,
        jwtString: String
    ) {
        every { jwtDecoder.decode(jwtString) } returns Jwt(
            jwtString,
            Instant.now(),
            Instant.now().plusSeconds(20),
            mapOf("header1" to "header1"),
            mapOf("claim1" to "claim1")
        )
    }

    fun clientIsAuthorized(
        clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService,
        clientId: String,
        sourceApplicationId: String
    ) {
        every { clientAuthorizationRequestService.getClientAuthorization(clientId) } returns Optional.of(
            SourceApplicationAuthorization.builder()
                .authorized(true)
                .clientId(clientId)
                .sourceApplicationId(sourceApplicationId)
                .build()
        )
    }

    fun clientIsNotAuthorized(
        clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService,
        clientId: String
    ) {
        every { clientAuthorizationRequestService.getClientAuthorization(clientId) } returns Optional.of(
            SourceApplicationAuthorization.builder()
                .authorized(false)
                .clientId(clientId)
                .build()
        )
    }

    fun authorizationRequestReturnsEmpty(
        clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService,
        clientId: String
    ) {
        every { clientAuthorizationRequestService.getClientAuthorization(clientId) } returns Optional.empty()
    }
}
