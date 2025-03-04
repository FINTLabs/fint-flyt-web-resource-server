package no.fintlabs.webresourceserver.security.client.sourceapplication

data class SourceApplicationAuthorization(
    var authorized: Boolean = false,
    var clientId: String? = null,
    var sourceApplicationId: String? = null,
)