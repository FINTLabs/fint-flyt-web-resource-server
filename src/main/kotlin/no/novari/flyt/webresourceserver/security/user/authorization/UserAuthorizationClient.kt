package no.novari.flyt.webresourceserver.security.user.authorization

import java.util.UUID

interface UserAuthorizationClient {
    fun getAuthorizedSourceApplicationIds(
        objectIdentifier: UUID,
        sourceApplicationIds: Set<Long>,
    ): Set<Long>
}

class UserAuthorizationClientException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
