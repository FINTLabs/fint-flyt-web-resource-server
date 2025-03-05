package no.fintlabs.webresourceserver.security.user

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.server.ResponseStatusException

class UserAuthorizationUtil {

    fun convertSourceApplicationIdsStringToList(authentication: Authentication): List<Long> {
        val jwt = authentication.principal as Jwt
        val sourceApplicationIds = jwt.getClaimAsString("sourceApplicationIds")

        if (sourceApplicationIds.isNullOrEmpty() || sourceApplicationIds.isBlank()) {
            return emptyList()
        }

        return sourceApplicationIds.split(",").map { it.toLong() }
    }

    fun checkIfUserHasAccessToSourceApplication(authentication: Authentication, sourceApplicationId: Long) {
        val allowedSourceApplicationIds = convertSourceApplicationIdsStringToList(authentication)
        if (allowedSourceApplicationIds.contains(sourceApplicationId)) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application with id=$sourceApplicationId"
            )
        }
    }

}