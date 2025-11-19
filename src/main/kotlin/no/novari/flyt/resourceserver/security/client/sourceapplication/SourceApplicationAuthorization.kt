package no.novari.flyt.resourceserver.security.client.sourceapplication

data class SourceApplicationAuthorization(
    val authorized: Boolean = false,
    val clientId: String? = null,
    val sourceApplicationId: Long? = null,
)
