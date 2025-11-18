package no.novari.flyt.resourceserver.security.client.sourceapplication

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class SourceApplicationAuthorization @JsonCreator constructor(
    @JsonProperty("authorized")
    val authorized: Boolean = false,
    @JsonProperty("clientId")
    val clientId: String? = null,
    @JsonProperty("sourceApplicationId")
    val sourceApplicationId: Long? = null
)
