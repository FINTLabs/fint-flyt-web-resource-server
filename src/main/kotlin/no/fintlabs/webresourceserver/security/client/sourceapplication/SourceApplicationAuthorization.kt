package no.fintlabs.webresourceserver.security.client.sourceapplication

data class SourceApplicationAuthorization(
    var authorized: Boolean = false,
    var clientId: String? = null,
    var sourceApplicationId: String? = null,
) {
    data class Builder(
        var authorized: Boolean = false,
        var clientId: String? = null,
        var sourceApplicationId: String? = null,
    ) {
        fun authorized(authorized: Boolean) = apply { this.authorized = authorized }
        fun clientId(clientId: String) = apply { this.clientId = clientId }
        fun sourceApplicationId(sourceApplicationId: String?) = apply { this.sourceApplicationId = sourceApplicationId }
        fun build() = SourceApplicationAuthorization(authorized, clientId, sourceApplicationId)
    }

    companion object {
        fun builder() = Builder()
    }
}