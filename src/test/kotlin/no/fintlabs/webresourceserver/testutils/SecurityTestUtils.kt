package no.fintlabs.webresourceserver.testutils

import io.mockk.every
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorization
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import java.time.Instant

object SecurityTestUtils {
    private const val HEADER_1 = "header1"
    private const val CLAIM_SUB = "sub"
    private const val CLAIM_ORGANIZATION_ID = "organizationid"
    private const val CLAIM_ORGANIZATION_NUMBER = "organizationnumber"
    private const val CLAIM_ROLES = "roles"
    private const val TOKEN_TTL_SECONDS = 20L

    fun clientIsAuthorized(
        clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService,
        clientId: String,
        sourceApplicationId: String,
    ) {
        every { clientAuthorizationRequestService.getClientAuthorization(clientId) } returns
            SourceApplicationAuthorization
                .builder()
                .authorized(true)
                .clientId(clientId)
                .sourceApplicationId(sourceApplicationId)
                .build()
    }

    fun clientIsNotAuthorized(
        clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService,
        clientId: String,
    ) {
        every { clientAuthorizationRequestService.getClientAuthorization(clientId) } returns
            SourceApplicationAuthorization
                .builder()
                .authorized(false)
                .clientId(clientId)
                .build()
    }

    fun authorizationRequestReturnsEmpty(
        clientAuthorizationRequestService: SourceApplicationAuthorizationRequestService,
        clientId: String,
    ) {
        every { clientAuthorizationRequestService.getClientAuthorization(clientId) } returns null
    }

    fun tokenContainsClientId(
        jwtDecoder: JwtDecoder,
        jwtString: String,
        clientId: String,
    ) {
        every { jwtDecoder.decode(jwtString) } returns
            Jwt(
                jwtString,
                Instant.now(),
                Instant.now().plusSeconds(TOKEN_TTL_SECONDS),
                mapOf(HEADER_1 to HEADER_1),
                mapOf(CLAIM_SUB to clientId),
            )
    }

    fun tokenDoesNotContainClientId(
        jwtDecoder: JwtDecoder,
        jwtString: String,
    ) {
        every { jwtDecoder.decode(jwtString) } returns
            Jwt(
                jwtString,
                Instant.now(),
                Instant.now().plusSeconds(TOKEN_TTL_SECONDS),
                mapOf(HEADER_1 to HEADER_1),
                mapOf("claim1" to "claim1"),
            )
    }

    fun tokenContainsOrgId(
        jwtDecoder: JwtDecoder,
        jwtString: String,
        orgId: String,
    ) {
        every { jwtDecoder.decode(jwtString) } returns
            Jwt(
                jwtString,
                Instant.now(),
                Instant.now().plusSeconds(TOKEN_TTL_SECONDS),
                mapOf(HEADER_1 to HEADER_1),
                mapOf(
                    CLAIM_ORGANIZATION_ID to orgId,
                    CLAIM_ORGANIZATION_NUMBER to "organizationNumber",
                    CLAIM_ROLES to emptyList<String>(),
                ),
            )
    }

    fun tokenContainsOrgIdAndRoles(
        jwtDecoder: JwtDecoder,
        jwtString: String,
        orgId: String,
        roles: List<String>,
    ) {
        every { jwtDecoder.decode(jwtString) } returns
            Jwt(
                jwtString,
                Instant.now(),
                Instant.now().plusSeconds(TOKEN_TTL_SECONDS),
                mapOf(HEADER_1 to HEADER_1),
                mapOf(
                    CLAIM_ORGANIZATION_ID to orgId,
                    CLAIM_ORGANIZATION_NUMBER to "organizationNumber",
                    CLAIM_ROLES to roles,
                ),
            )
    }
}
