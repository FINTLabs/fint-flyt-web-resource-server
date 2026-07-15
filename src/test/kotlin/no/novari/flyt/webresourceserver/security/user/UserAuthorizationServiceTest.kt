package no.novari.flyt.webresourceserver.security.user

import no.novari.flyt.webresourceserver.security.AuthorityMappingService
import no.novari.flyt.webresourceserver.security.AuthorityPrefix
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClient
import no.novari.flyt.webresourceserver.security.user.authorization.UserAuthorizationClientException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class UserAuthorizationServiceTest {
    private lateinit var authorityMappingService: AuthorityMappingService
    private lateinit var userAuthorizationClient: UserAuthorizationClient
    private lateinit var service: UserAuthorizationService

    @BeforeEach
    fun setup() {
        authorityMappingService = mock(AuthorityMappingService::class.java)
        userAuthorizationClient = mock(UserAuthorizationClient::class.java)
        service = UserAuthorizationService(authorityMappingService, userAuthorizationClient)
    }

    @Test
    fun `authorized source application does not throw`() {
        val objectIdentifier = UUID.randomUUID()
        val authentication = authentication(objectIdentifier)
        `when`(userAuthorizationClient.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(3L)))
            .thenReturn(setOf(3L))

        service.checkIfUserHasAccessToSourceApplication(authentication, 3L)

        verify(userAuthorizationClient).getAuthorizedSourceApplicationIds(objectIdentifier, setOf(3L))
    }

    @Test
    fun `unauthorized source application throws forbidden`() {
        val objectIdentifier = UUID.randomUUID()
        `when`(userAuthorizationClient.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(3L)))
            .thenReturn(emptySet())

        val exception =
            assertThrows<ResponseStatusException> {
                service.checkIfUserHasAccessToSourceApplication(authentication(objectIdentifier), 3L)
            }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `authorization service failure throws service unavailable`() {
        val objectIdentifier = UUID.randomUUID()
        `when`(userAuthorizationClient.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(3L)))
            .thenThrow(UserAuthorizationClientException("unavailable", IllegalStateException()))

        val exception =
            assertThrows<ResponseStatusException> {
                service.checkIfUserHasAccessToSourceApplication(authentication(objectIdentifier), 3L)
            }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun `candidate lookup returns only authorized IDs`() {
        val objectIdentifier = UUID.randomUUID()
        `when`(
            userAuthorizationClient.getAuthorizedSourceApplicationIds(objectIdentifier, setOf(1L, 2L, 3L)),
        ).thenReturn(setOf(1L, 3L))

        assertThat(
            service.getUserAuthorizedSourceApplicationIds(authentication(objectIdentifier), setOf(1L, 2L, 3L)),
        ).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    fun `userHasRole returns true when role is present`() {
        val authentication = mock(Authentication::class.java)
        val authorities = mock(Collection::class.java) as Collection<GrantedAuthority>
        `when`(authentication.authorities).thenReturn(authorities)
        `when`(
            authorityMappingService.extractStringValues(
                AuthorityPrefix.ROLE,
                authorities,
            ),
        ).thenReturn(setOf(UserRole.DEVELOPER.name))

        assertThat(service.userHasRole(authentication, UserRole.DEVELOPER)).isTrue()
    }

    private fun authentication(objectIdentifier: UUID): JwtAuthenticationToken =
        JwtAuthenticationToken(
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .claim(UserClaim.OBJECT_IDENTIFIER.tokenClaimName, objectIdentifier.toString())
                .build(),
        )
}
