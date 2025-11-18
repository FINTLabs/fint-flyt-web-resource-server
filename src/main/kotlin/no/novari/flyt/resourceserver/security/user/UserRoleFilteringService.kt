package no.novari.flyt.resourceserver.security.user

import no.novari.flyt.resourceserver.security.properties.InternalApiSecurityProperties
import org.slf4j.LoggerFactory

class UserRoleFilteringService(
    private val internalApiSecurityProperties: InternalApiSecurityProperties
) {

    fun filter(roleValues: Collection<String>, organizationId: String): Set<UserRole> {
        log.debug("roleValues : {}", roleValues)
        if (roleValues.isEmpty()) {
            return emptySet()
        }

        val filteredUserRoles = roleValues
            .mapNotNull(UserRole::getUserRoleFromValue)
            .toMutableSet()

        log.debug("filteredUserRoles before filter : {}", filteredUserRoles)
        if (filteredUserRoles.isEmpty()) {
            return emptySet()
        }

        val roleFilter = internalApiSecurityProperties.userRoleFilterPerOrgId
            .getOrDefault(organizationId, emptySet())

        log.debug("roleFilter: {}", roleFilter)

        filteredUserRoles.retainAll(roleFilter)

        log.debug("filteredUserRoles after filter : {}", filteredUserRoles)

        return filteredUserRoles
    }

    private companion object {
        private val log = LoggerFactory.getLogger(UserRoleFilteringService::class.java)
    }
}
