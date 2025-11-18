package no.novari.flyt.resourceserver.security.user

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class UserRoleAuthorityMappingService(
    private val authorityMappingService: AuthorityMappingService,
) {
    fun createRoleAuthorities(roles: Collection<UserRole>): Set<GrantedAuthority> {
        return roles.mapTo(mutableSetOf(), ::createRoleAuthority)
    }

    fun createRoleAuthority(role: UserRole): GrantedAuthority {
        return SimpleGrantedAuthority(createRoleAuthorityString(role))
    }

    fun createRoleAuthorityStrings(roles: Collection<UserRole>): Set<String> {
        return roles.mapTo(mutableSetOf(), ::createRoleAuthorityString)
    }

    fun createRoleAuthorityString(role: UserRole): String {
        return authorityMappingService.toAuthority(
            AuthorityPrefix.ROLE,
            role.name,
        )
    }
}
