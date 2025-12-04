package no.novari.flyt.webresourceserver.security.user

import no.novari.flyt.webresourceserver.security.properties.InternalApiSecurityProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class UserRoleFilteringServiceTest {
    private val internalApiSecurityProperties = Mockito.mock(InternalApiSecurityProperties::class.java)
    private val service = UserRoleFilteringService(internalApiSecurityProperties)

    @Test
    fun `no roles should return empty set`() {
        val filter = service.filter(emptySet(), "testOrganizationId")

        assertThat(filter).isEmpty()
        Mockito.verifyNoInteractions(internalApiSecurityProperties)
    }

    @Test
    fun `unknown roles should return empty set`() {
        val filter = service.filter(setOf("unknownRole1", "unknownRole2"), "testOrganizationId")

        assertThat(filter).isEmpty()
        Mockito.verifyNoInteractions(internalApiSecurityProperties)
    }

    @Test
    fun `known and approved role should be returned`() {
        val organizationId = "testOrganizationId"
        Mockito
            .`when`(internalApiSecurityProperties.userRoleFilterPerOrgId)
            .thenReturn(mapOf(organizationId to setOf(UserRole.USER)))

        val filter = service.filter(setOf(UserRole.USER.claimValue), organizationId)

        assertThat(filter).isEqualTo(setOf(UserRole.USER))
    }

    @Test
    fun `known role but empty filter should return empty`() {
        val organizationId = "testOrganizationId"
        Mockito
            .`when`(internalApiSecurityProperties.userRoleFilterPerOrgId)
            .thenReturn(mapOf(organizationId to emptySet()))

        val filter = service.filter(setOf(UserRole.USER.claimValue), organizationId)

        assertThat(filter).isEmpty()
    }

    @Test
    fun `no filter for organization should return empty`() {
        Mockito
            .`when`(internalApiSecurityProperties.userRoleFilterPerOrgId)
            .thenReturn(mapOf("otherOrg" to setOf(UserRole.USER)))

        val filter = service.filter(setOf(UserRole.USER.claimValue), "missingOrg")

        assertThat(filter).isEmpty()
    }

    @Test
    fun `mixed roles should only return approved roles`() {
        val organizationId = "testOrganizationId1"
        Mockito
            .`when`(internalApiSecurityProperties.userRoleFilterPerOrgId)
            .thenReturn(
                mapOf(
                    organizationId to setOf(UserRole.ADMIN),
                    "testOrganizationId2" to setOf(UserRole.ADMIN, UserRole.USER),
                ),
            )

        val filter =
            service.filter(
                setOf(UserRole.ADMIN.claimValue, UserRole.USER.claimValue, "unknownRole1"),
                organizationId,
            )

        assertThat(filter).isEqualTo(setOf(UserRole.ADMIN))
    }
}
