package no.novari.flyt.resourceserver.security.user.permission

import java.util.UUID

data class UserPermission(
    val objectIdentifier: UUID,
    val sourceApplicationIds: Set<Long>,
)
