package no.novari.flyt.resourceserver.security.integration.values

enum class PersonalTokenOrgId(val claimValue: String) {
    WITH_ALL_USER_ACCESS("domain-with-all-user-access.no"),
    WITH_ADMIN_AND_DEV_ACCESS("domain-with-admin-and-dev-access.no"),
    WITH_NO_ACCESS("domain-without-role-access.no")
}
