package no.novari.flyt.webresourceserver.security.user

public enum class UserClaim(
    public val tokenClaimName: String,
) {
    ORGANIZATION_ID("organizationid"),
    OBJECT_IDENTIFIER("objectidentifier"),
    ROLES("roles"),
    SOURCE_APPLICATION_IDS("sourceApplicationIds"),
}
