package no.novari.flyt.resourceserver.security.user

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.core.GrantedAuthority

class UserRoleAuthorityMappingServiceTest {
    private val authorityMappingService = Mockito.mock(AuthorityMappingService::class.java)
    private val service = UserRoleAuthorityMappingService(authorityMappingService)

    @Test
    fun `createRoleAuthorities should convert each role`() {
        Mockito
            .`when`(
                authorityMappingService.toAuthority(AuthorityPrefix.ROLE, UserRole.USER.name),
            ).thenReturn("ROLE_USER")
        Mockito
            .`when`(
                authorityMappingService.toAuthority(AuthorityPrefix.ROLE, UserRole.DEVELOPER.name),
            ).thenReturn("ROLE_DEVELOPER")

        val roleAuthorities = service.createRoleAuthorities(setOf(UserRole.USER, UserRole.DEVELOPER))

        assertThat(roleAuthorities.map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("ROLE_USER", "ROLE_DEVELOPER")
    }

    @Test
    fun `createRoleAuthority should convert single role`() {
        Mockito
            .`when`(
                authorityMappingService.toAuthority(AuthorityPrefix.ROLE, UserRole.USER.name),
            ).thenReturn("ROLE_USER")

        val roleAuthority = service.createRoleAuthority(UserRole.USER)

        assertThat(roleAuthority.authority).isEqualTo("ROLE_USER")
    }
}
