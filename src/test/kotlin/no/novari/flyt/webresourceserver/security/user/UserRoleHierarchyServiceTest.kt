package no.novari.flyt.webresourceserver.security.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserRoleHierarchyServiceTest {
    private lateinit var userRoleHierarchyService: UserRoleHierarchyService

    @BeforeEach
    fun setUp() {
        userRoleHierarchyService = UserRoleHierarchyService()
    }

    @Test
    fun `user should only imply user`() {
        assertThat(userRoleHierarchyService.getProvidedAndImpliedRoles(listOf(UserRole.USER)))
            .isEqualTo(setOf(UserRole.USER))
    }

    @Test
    fun `admin should imply admin and user`() {
        assertThat(userRoleHierarchyService.getProvidedAndImpliedRoles(listOf(UserRole.ADMIN)))
            .isEqualTo(setOf(UserRole.ADMIN, UserRole.USER))
    }

    @Test
    fun `developer should imply developer admin and user`() {
        assertThat(userRoleHierarchyService.getProvidedAndImpliedRoles(listOf(UserRole.DEVELOPER)))
            .isEqualTo(setOf(UserRole.DEVELOPER, UserRole.ADMIN, UserRole.USER))
    }
}
