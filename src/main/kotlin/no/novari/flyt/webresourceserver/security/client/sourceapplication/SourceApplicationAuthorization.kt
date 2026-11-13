package no.novari.flyt.webresourceserver.security.client.sourceapplication

internal data class SourceApplicationAuthorization(
    val authorized: Boolean = false,
    val clientId: String? = null,
    val sourceApplicationId: Long? = null,
)
