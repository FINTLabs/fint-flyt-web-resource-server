package no.novari.flyt.webresourceserver.security.user

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

class UserJwtConverterTest {
    private lateinit var userRoleAuthorityMappingService: UserRoleAuthorityMappingService
    private lateinit var userRoleFilteringService: UserRoleFilteringService
    private lateinit var userRoleHierarchyService: UserRoleHierarchyService
    private lateinit var converter: UserJwtConverter
    private lateinit var jwt: Jwt

    @BeforeEach
    fun setUp() {
        userRoleAuthorityMappingService = mock(UserRoleAuthorityMappingService::class.java)
        userRoleFilteringService = mock(UserRoleFilteringService::class.java)
        userRoleHierarchyService = mock(UserRoleHierarchyService::class.java)
        converter =
            UserJwtConverter(
                userRoleFilteringService,
                userRoleHierarchyService,
                userRoleAuthorityMappingService,
            )
        jwt = mock(Jwt::class.java)
    }

    @Test
    fun `no organization id claim throws exception`() {
        `when`(jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName))
            .thenReturn(null)

        assertThatThrownBy { converter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)
    }

    @Test
    fun `no object identifier claim throws exception`() {
        `when`(jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName))
            .thenReturn("testOrganizationId")

        assertThatThrownBy { converter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)
    }

    @Test
    fun `valid token without roles creates no source application authorities`() {
        val objectIdentifier = UUID.randomUUID()
        `when`(jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName))
            .thenReturn("testOrganizationId")
        `when`(jwt.getClaimAsString(UserClaim.OBJECT_IDENTIFIER.tokenClaimName))
            .thenReturn(objectIdentifier.toString())
        `when`(jwt.getClaimAsStringList(UserClaim.ROLES.tokenClaimName)).thenReturn(emptyList())

        val authentication = converter.convert(jwt)

        assertThat(authentication).isInstanceOf(JwtAuthenticationToken::class.java)
        assertThat(authentication.authorities).isEmpty()
        assertThat(authentication.isAuthenticated).isTrue()
        verifyNoInteractions(
            userRoleAuthorityMappingService,
            userRoleFilteringService,
            userRoleHierarchyService,
        )
    }

    @Test
    fun `valid token adds mapped role authorities`() {
        val objectIdentifier = UUID.randomUUID()
        val roleClaims = setOf("TEST_ROLE_1", "TEST_ROLE_2")
        val roleAuthority = mock(GrantedAuthority::class.java)
        `when`(jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName))
            .thenReturn("testOrganizationId")
        `when`(jwt.getClaimAsString(UserClaim.OBJECT_IDENTIFIER.tokenClaimName))
            .thenReturn(objectIdentifier.toString())
        `when`(jwt.getClaimAsStringList(UserClaim.ROLES.tokenClaimName)).thenReturn(roleClaims.toList())
        `when`(userRoleFilteringService.filter(roleClaims, "testOrganizationId"))
            .thenReturn(setOf(UserRole.ADMIN))
        `when`(userRoleHierarchyService.getProvidedAndImpliedRoles(setOf(UserRole.ADMIN)))
            .thenReturn(setOf(UserRole.ADMIN, UserRole.USER))
        `when`(
            userRoleAuthorityMappingService.createRoleAuthorities(setOf(UserRole.ADMIN, UserRole.USER)),
        ).thenReturn(setOf(roleAuthority))

        val authentication = converter.convert(jwt)

        assertThat(authentication.authorities).containsExactly(roleAuthority)
    }
}
