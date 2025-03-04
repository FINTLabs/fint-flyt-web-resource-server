package no.fintlabs.webresourceserver.security.user.userpermission

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class UserPermission @JsonCreator constructor(
    @JsonProperty("objectIdentifier") val objectIdentifier: UUID,
    @JsonProperty("sourceApplicationIds") val sourceApplicationIds: List<Long>,
)