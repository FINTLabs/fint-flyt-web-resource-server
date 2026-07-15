package no.novari.flyt.webresourceserver.security.user

import no.novari.flyt.webresourceserver.security.AuthorityMappingService
import no.novari.flyt.webresourceserver.security.AuthorityPrefix
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClient
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClientException
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class UserAuthorizationService(
    private val authorityMappingService: AuthorityMappingService,
    private val userAuthorizationClient: UserAuthorizationClient,
) {
    fun getUserAuthorizedSourceApplicationIds(
        authentication: Authentication,
        sourceApplicationIds: Set<Long>,
    ): Set<Long> =
        try {
            userAuthorizationClient.getAuthorizedSourceApplicationIds(
                objectIdentifier(authentication),
                sourceApplicationIds,
            )
        } catch (exception: UserAuthorizationClientException) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to verify user authorization",
                exception,
            )
        }

    fun checkIfUserHasAccessToSourceApplication(
        authentication: Authentication,
        sourceApplicationId: Long,
    ) {
        val authorizedSourceApplicationIds =
            getUserAuthorizedSourceApplicationIds(
                authentication,
                setOf(sourceApplicationId),
            )
        if (!authorizedSourceApplicationIds.contains(sourceApplicationId)) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application " +
                    "with id=$sourceApplicationId",
            )
        }
    }

    fun userHasRole(
        authentication: Authentication,
        role: UserRole,
    ): Boolean {
        return authorityMappingService
            .extractStringValues(
                AuthorityPrefix.ROLE,
                authentication.authorities,
            ).contains(role.name)
    }

    private fun objectIdentifier(authentication: Authentication): UUID {
        val jwtAuthentication =
            authentication as? JwtAuthenticationToken
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "User authentication is required")
        val value =
            jwtAuthentication.token.getClaimAsString(UserClaim.OBJECT_IDENTIFIER.tokenClaimName)
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "User object identifier is required")
        return try {
            UUID.fromString(value)
        } catch (exception: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "User object identifier is invalid", exception)
        }
    }
}
