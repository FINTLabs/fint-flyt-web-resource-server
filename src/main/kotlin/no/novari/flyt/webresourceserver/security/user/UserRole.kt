package no.novari.flyt.webresourceserver.security.user

public enum class UserRole(
    public val claimValue: String,
) {
    USER("https://role-catalog.vigoiks.no/vigo/flyt/user"),
    DEVELOPER("https://role-catalog.vigoiks.no/vigo/flyt/developer"),
    ADMIN("https://role-catalog.vigoiks.no/vigo/flyt/admin"),
    ;

    public companion object {
        private val userRoleByClaimValue = entries.associateBy(UserRole::claimValue)

        public fun getUserRoleFromValue(roleValue: String): UserRole? = userRoleByClaimValue[roleValue]
    }
}
