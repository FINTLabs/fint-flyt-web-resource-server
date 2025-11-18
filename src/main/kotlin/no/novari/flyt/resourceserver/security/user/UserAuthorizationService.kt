package no.novari.flyt.resourceserver.security.user

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException

class UserAuthorizationService(
    private val authorityMappingService: AuthorityMappingService,
) {
    fun getUserAuthorizedSourceApplicationIds(authentication: Authentication): Set<Long> {
        return authorityMappingService.extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authentication.authorities,
        )
    }

    fun checkIfUserHasAccessToSourceApplication(
        authentication: Authentication,
        sourceApplicationId: Long,
    ) {
        if (!getUserAuthorizedSourceApplicationIds(authentication).contains(sourceApplicationId)) {
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
    ): Boolean =
        authorityMappingService
            .extractStringValues(
                AuthorityPrefix.ROLE,
                authentication.authorities,
            ).contains(role.name)
}
