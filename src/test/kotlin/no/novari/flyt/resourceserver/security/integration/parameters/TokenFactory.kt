package no.novari.flyt.resourceserver.security.integration.parameters

import no.novari.flyt.resourceserver.security.integration.values.ClientId
import no.novari.flyt.resourceserver.security.integration.values.PersonalTokenObjectIdentifier
import no.novari.flyt.resourceserver.security.integration.values.PersonalTokenOrgId
import no.novari.flyt.resourceserver.security.user.UserClaim
import no.novari.flyt.resourceserver.security.user.UserRole
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import java.time.Instant

object TokenFactory {
    fun createPersonalToken(
        orgId: PersonalTokenOrgId,
        objectIdentifier: PersonalTokenObjectIdentifier,
        roles: Set<UserRole>,
    ): TokenWrapper =
        TokenWrapper(
            "{orgId: $orgId, objId: $objectIdentifier, roles: $roles}",
            createJwt(
                mapOf(
                    UserClaim.ORGANIZATION_ID.tokenClaimName to orgId.claimValue,
                    UserClaim.OBJECT_IDENTIFIER.tokenClaimName to objectIdentifier.getClaimValue(),
                    UserClaim.ROLES.tokenClaimName to roles.map(UserRole::claimValue).toSet(),
                ),
            ),
        )

    fun createClientToken(clientId: ClientId): TokenWrapper {
        return TokenWrapper(
            "{sub: $clientId}",
            createJwt(mapOf(JwtClaimNames.SUB to clientId.claimValue)),
        )
    }

    private fun createJwt(claims: Map<String, Any?>): Jwt {
        return Jwt(
            "testTokenValue",
            Instant.now(),
            Instant.now().plusMillis(20_000),
            mapOf("header1" to "header1"),
            claims,
        )
    }
}
