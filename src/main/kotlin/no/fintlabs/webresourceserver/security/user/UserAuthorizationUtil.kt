package no.fintlabs.webresourceserver.security.user

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.server.ResponseStatusException

class UserAuthorizationUtil {
    companion object {
        private const val CLAIM_SOURCE_APPLICATION_IDS = "sourceApplicationIds"
    }

    fun convertSourceApplicationIdsStringToList(authentication: Authentication): List<Long> {
        val jwt = authentication.principal as? Jwt ?: return emptyList()
        val sourceApplicationIds = jwt.getClaimAsString(CLAIM_SOURCE_APPLICATION_IDS)
        if (sourceApplicationIds.isNullOrBlank()) return emptyList()
        return sourceApplicationIds.split(',').map { it.trim().toLong() }
    }

    fun checkIfUserHasAccessToSourceApplication(
        authentication: Authentication,
        sourceApplicationId: Long,
    ) {
        val allowedSourceApplicationIds = convertSourceApplicationIdsStringToList(authentication)
        if (sourceApplicationId !in allowedSourceApplicationIds) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application with id=$sourceApplicationId",
            )
        }
    }
}
