package no.novari.flyt.webresourceserver.security.user.authorization

import java.util.UUID

public interface UserAuthorizationClient {
    public fun getAuthorizedSourceApplicationIds(
        objectIdentifier: UUID,
        sourceApplicationIds: Set<Long>,
    ): Set<Long>
}

public class UserAuthorizationClientException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
