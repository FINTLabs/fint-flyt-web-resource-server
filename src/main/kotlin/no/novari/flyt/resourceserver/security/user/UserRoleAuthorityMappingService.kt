package no.novari.flyt.resourceserver.security.user

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class UserRoleAuthorityMappingService(
    private val authorityMappingService: AuthorityMappingService
) {

    fun createRoleAuthorities(roles: Collection<UserRole>): Set<GrantedAuthority> =
        roles.mapTo(mutableSetOf(), ::createRoleAuthority)

    fun createRoleAuthority(role: UserRole): GrantedAuthority =
        SimpleGrantedAuthority(createRoleAuthorityString(role))

    fun createRoleAuthorityStrings(roles: Collection<UserRole>): Set<String> =
        roles.mapTo(mutableSetOf(), ::createRoleAuthorityString)

    fun createRoleAuthorityString(role: UserRole): String = authorityMappingService.toAuthority(
        AuthorityPrefix.ROLE,
        role.name
    )
}
