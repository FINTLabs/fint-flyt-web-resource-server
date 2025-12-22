package no.novari.flyt.webresourceserver.security.user

enum class UserClaim(
    val tokenClaimName: String,
) {
    ORGANIZATION_ID("organizationid"),
    OBJECT_IDENTIFIER("objectidentifier"),
    ROLES("roles"),
    SOURCE_APPLICATION_IDS("sourceApplicationIds"),
}
