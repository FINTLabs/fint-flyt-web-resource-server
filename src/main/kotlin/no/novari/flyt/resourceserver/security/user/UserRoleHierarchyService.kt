package no.novari.flyt.resourceserver.security.user

class UserRoleHierarchyService {
    fun getProvidedAndImpliedRoles(roles: Collection<UserRole>): Set<UserRole> {
        return roles.flatMapTo(mutableSetOf(), ::getProvidedAndImpliedRoles)
    }

    private fun getProvidedAndImpliedRoles(role: UserRole): Set<UserRole> {
        val userRoles =
            IMPLIED_ROLES_PER_ROLE
                .getValue(role)
                .flatMapTo(mutableSetOf(), ::getProvidedAndImpliedRoles)
        userRoles.add(role)
        return userRoles
    }

    private companion object {
        private val IMPLIED_ROLES_PER_ROLE =
            mapOf(
                UserRole.USER to setOf(),
                UserRole.ADMIN to setOf(UserRole.USER),
                UserRole.DEVELOPER to setOf(UserRole.ADMIN),
            )
    }
}
