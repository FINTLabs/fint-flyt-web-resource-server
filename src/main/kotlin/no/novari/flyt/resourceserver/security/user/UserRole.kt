package no.novari.flyt.resourceserver.security.user

enum class UserRole(
    val claimValue: String,
) {
    USER("https://role-catalog.vigoiks.no/vigo/flyt/user"),
    DEVELOPER("https://role-catalog.vigoiks.no/vigo/flyt/developer"),
    ADMIN("https://role-catalog.vigoiks.no/vigo/flyt/admin"),
    ;

    companion object {
        private val userRoleByClaimValue = entries.associateBy(UserRole::claimValue)

        fun getUserRoleFromValue(roleValue: String): UserRole? = userRoleByClaimValue[roleValue]
    }
}
