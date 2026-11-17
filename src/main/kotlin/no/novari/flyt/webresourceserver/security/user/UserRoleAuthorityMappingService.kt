package no.novari.flyt.webresourceserver.security.user

import no.novari.flyt.webresourceserver.security.AuthorityMappingService
import no.novari.flyt.webresourceserver.security.AuthorityPrefix
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

public class UserRoleAuthorityMappingService(
    private val authorityMappingService: AuthorityMappingService,
) {
    public fun createRoleAuthorities(roles: Collection<UserRole>): Set<GrantedAuthority> {
        return roles.mapTo(mutableSetOf(), ::createRoleAuthority)
    }

    public fun createRoleAuthority(role: UserRole): GrantedAuthority {
        return SimpleGrantedAuthority(createRoleAuthorityString(role))
    }

    public fun createRoleAuthorityStrings(roles: Collection<UserRole>): Set<String> {
        return roles.mapTo(mutableSetOf(), ::createRoleAuthorityString)
    }

    public fun createRoleAuthorityString(role: UserRole): String {
        return authorityMappingService.toAuthority(
            AuthorityPrefix.ROLE,
            role.name,
        )
    }
}
