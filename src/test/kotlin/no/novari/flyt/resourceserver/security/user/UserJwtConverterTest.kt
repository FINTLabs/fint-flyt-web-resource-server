package no.novari.flyt.resourceserver.security.user

import no.novari.cache.FintCache
import no.novari.flyt.resourceserver.security.client.sourceapplication.SourceApplicationAuthorityMappingService
import no.novari.flyt.resourceserver.security.user.permission.UserPermission
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.Optional
import java.util.UUID

@Suppress("UNCHECKED_CAST")
class UserJwtConverterTest {
    private lateinit var userPermissionCache: FintCache<UUID, UserPermission>
    private lateinit var sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService
    private lateinit var userRoleAuthorityMappingService: UserRoleAuthorityMappingService
    private lateinit var userRoleFilteringService: UserRoleFilteringService
    private lateinit var userRoleHierarchyService: UserRoleHierarchyService
    private lateinit var converter: UserJwtConverter
    private lateinit var jwt: Jwt

    @BeforeEach
    fun setUp() {
        userPermissionCache = Mockito.mock(FintCache::class.java) as FintCache<UUID, UserPermission>
        sourceApplicationAuthorityMappingService =
            Mockito.mock(SourceApplicationAuthorityMappingService::class.java)
        userRoleAuthorityMappingService = Mockito.mock(UserRoleAuthorityMappingService::class.java)
        userRoleFilteringService = Mockito.mock(UserRoleFilteringService::class.java)
        userRoleHierarchyService = Mockito.mock(UserRoleHierarchyService::class.java)
        converter =
            UserJwtConverter(
                userPermissionCache,
                userRoleFilteringService,
                sourceApplicationAuthorityMappingService,
                userRoleHierarchyService,
                userRoleAuthorityMappingService,
            )
        jwt = Mockito.mock(Jwt::class.java)
    }

    @Test
    fun `no organization id claim throws exception`() {
        Mockito
            .`when`(jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName))
            .thenReturn(null)

        assertThatThrownBy { converter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)
    }

    @Test
    fun `no object identifier claim throws exception`() {
        Mockito
            .`when`(jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName))
            .thenReturn("testOrganizationId")

        assertThatThrownBy { converter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)
    }

    @Test
    fun `valid token without permissions should return no authorities`() {
        Mockito
            .`when`(jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName))
            .thenReturn("testOrganizationId")
        val objectIdentifier = UUID.fromString("377cfaae-ef8f-4060-86b6-1cd083bfde07")
        Mockito
            .`when`(jwt.getClaimAsString(UserClaim.OBJECT_IDENTIFIER.tokenClaimName))
            .thenReturn(objectIdentifier.toString())

        Mockito
            .`when`(userPermissionCache.getOptional(objectIdentifier))
            .thenReturn(Optional.empty())
        Mockito.`when`(jwt.getClaimAsStringList(UserClaim.ROLES.tokenClaimName)).thenReturn(listOf())

        val authentication = converter.convert(jwt)
        assertThat(authentication).isInstanceOf(JwtAuthenticationToken::class.java)
        assertThat(authentication.authorities).isEmpty()
        assertThat(authentication.isAuthenticated).isTrue()

        Mockito.verify(jwt).getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName)
        Mockito.verify(jwt).getClaimAsString(UserClaim.OBJECT_IDENTIFIER.tokenClaimName)
        Mockito.verify(userPermissionCache).getOptional(objectIdentifier)
        Mockito.verify(jwt).getClaimAsStringList(UserClaim.ROLES.tokenClaimName)
        Mockito.verifyNoMoreInteractions(
            userRoleAuthorityMappingService,
            sourceApplicationAuthorityMappingService,
            userPermissionCache,
            userRoleFilteringService,
        )
    }

    @Test
    fun `valid token with permissions adds authorities`() {
        Mockito
            .`when`(jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName))
            .thenReturn("testOrganizationId")
        val objectIdentifier = UUID.fromString("377cfaae-ef8f-4060-86b6-1cd083bfde07")
        Mockito
            .`when`(jwt.getClaimAsString(UserClaim.OBJECT_IDENTIFIER.tokenClaimName))
            .thenReturn(objectIdentifier.toString())

        val userPermission = Mockito.mock(UserPermission::class.java)
        val sourceApplicationIds = setOf(1234L)
        Mockito.`when`(userPermission.sourceApplicationIds).thenReturn(sourceApplicationIds)

        val grantedAuthority1 = Mockito.mock(GrantedAuthority::class.java)
        val grantedAuthority2 = Mockito.mock(GrantedAuthority::class.java)
        Mockito
            .`when`(
                sourceApplicationAuthorityMappingService.createSourceApplicationAuthorities(sourceApplicationIds),
            ).thenReturn(setOf(grantedAuthority1, grantedAuthority2))

        Mockito
            .`when`(userPermissionCache.getOptional(objectIdentifier))
            .thenReturn(Optional.of(userPermission))

        val roleClaims = setOf("TEST_ROLE_1", "TEST_ROLE_2")
        Mockito
            .`when`(jwt.getClaimAsStringList(UserClaim.ROLES.tokenClaimName))
            .thenReturn(roleClaims.toList())

        Mockito
            .`when`(userRoleFilteringService.filter(roleClaims, "testOrganizationId"))
            .thenReturn(setOf(UserRole.ADMIN))
        Mockito
            .`when`(userRoleHierarchyService.getProvidedAndImpliedRoles(setOf(UserRole.ADMIN)))
            .thenReturn(setOf(UserRole.ADMIN, UserRole.USER))

        val roleAuthority = Mockito.mock(GrantedAuthority::class.java)
        Mockito
            .`when`(
                userRoleAuthorityMappingService.createRoleAuthorities(setOf(UserRole.ADMIN, UserRole.USER)),
            ).thenReturn(setOf(roleAuthority))

        val authentication = converter.convert(jwt)
        assertThat(authentication).isInstanceOf(JwtAuthenticationToken::class.java)
        assertThat(authentication.authorities).containsExactlyInAnyOrder(
            grantedAuthority1,
            grantedAuthority2,
            roleAuthority,
        )
        assertThat(authentication.isAuthenticated).isTrue()

        Mockito.verify(jwt).getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName)
        Mockito.verify(jwt).getClaimAsString(UserClaim.OBJECT_IDENTIFIER.tokenClaimName)
        Mockito.verify(userPermission).sourceApplicationIds
        Mockito
            .verify(sourceApplicationAuthorityMappingService)
            .createSourceApplicationAuthorities(sourceApplicationIds)
        Mockito.verify(userPermissionCache).getOptional(objectIdentifier)
        Mockito.verify(jwt).getClaimAsStringList(UserClaim.ROLES.tokenClaimName)
        Mockito.verify(userRoleFilteringService).filter(roleClaims, "testOrganizationId")
        Mockito.verify(userRoleHierarchyService).getProvidedAndImpliedRoles(setOf(UserRole.ADMIN))
        Mockito
            .verify(userRoleAuthorityMappingService)
            .createRoleAuthorities(setOf(UserRole.ADMIN, UserRole.USER))
        Mockito.verifyNoMoreInteractions(
            userPermission,
            userRoleAuthorityMappingService,
            sourceApplicationAuthorityMappingService,
            userPermissionCache,
            userRoleFilteringService,
            userRoleHierarchyService,
        )
    }
}
