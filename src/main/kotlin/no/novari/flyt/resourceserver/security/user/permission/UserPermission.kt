package no.novari.flyt.resourceserver.security.user.permission

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class UserPermission
    @JsonCreator
    constructor(
        @JsonProperty("objectIdentifier")
        val objectIdentifier: UUID,
        @JsonProperty("sourceApplicationIds")
        val sourceApplicationIds: Set<Long>,
    )
