package no.novari.flyt.resourceserver.security.user

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.web.server.ResponseStatusException

class UserAuthorizationServiceTest {
    private lateinit var authorityMappingService: AuthorityMappingService
    private lateinit var service: UserAuthorizationService

    @BeforeEach
    fun setup() {
        authorityMappingService = Mockito.mock(AuthorityMappingService::class.java)
        service = UserAuthorizationService(authorityMappingService)
    }

    @Test
    fun `no authorized ids should throw forbidden`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)
        Mockito.`when`(
            authorityMappingService.extractLongValues(
                AuthorityPrefix.SOURCE_APPLICATION_ID,
                authorities,
            ),
        ).thenReturn(emptySet())

        val exception =
            org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException::class.java) {
                service.checkIfUserHasAccessToSourceApplication(authentication, 1L)
            }
        assertThat(exception.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        Mockito.verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authorityMappingService)
    }

    @Test
    fun `authorized ids not containing target should throw forbidden`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)
        Mockito.`when`(
            authorityMappingService.extractLongValues(
                AuthorityPrefix.SOURCE_APPLICATION_ID,
                authorities,
            ),
        ).thenReturn(setOf(2L, 3L))

        val exception =
            org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException::class.java) {
                service.checkIfUserHasAccessToSourceApplication(authentication, 1L)
            }
        assertThat(exception.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        Mockito.verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authorityMappingService)
    }

    @Test
    fun `authorized ids containing target should not throw`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)
        Mockito.`when`(
            authorityMappingService.extractLongValues(
                AuthorityPrefix.SOURCE_APPLICATION_ID,
                authorities,
            ),
        ).thenReturn(setOf(2L, 3L))

        org.junit.jupiter.api.Assertions.assertDoesNotThrow {
            service.checkIfUserHasAccessToSourceApplication(authentication, 3L)
        }

        Mockito.verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authorityMappingService)
    }

    @Test
    fun `userHasRole returns false with no roles`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)

        Mockito.`when`(
            authorityMappingService.extractStringValues(
                AuthorityPrefix.ROLE,
                authorities,
            ),
        ).thenReturn(emptySet())

        assertThat(service.userHasRole(authentication, UserRole.USER)).isFalse()

        Mockito.verify(authorityMappingService).extractStringValues(
            AuthorityPrefix.ROLE,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authorityMappingService)
    }

    @Test
    fun `userHasRole returns false when role missing`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)

        Mockito.`when`(
            authorityMappingService.extractStringValues(
                AuthorityPrefix.ROLE,
                authorities,
            ),
        ).thenReturn(setOf("roleAuthorityValue1", "roleAuthorityValue2"))

        assertThat(service.userHasRole(authentication, UserRole.DEVELOPER)).isFalse()

        Mockito.verify(authorityMappingService).extractStringValues(
            AuthorityPrefix.ROLE,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authorityMappingService)
    }

    @Test
    fun `userHasRole returns true when role present`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)

        Mockito.`when`(
            authorityMappingService.extractStringValues(
                AuthorityPrefix.ROLE,
                authorities,
            ),
        ).thenReturn(setOf("roleAuthorityValue1", UserRole.DEVELOPER.name))

        assertThat(service.userHasRole(authentication, UserRole.DEVELOPER)).isTrue()

        Mockito.verify(authorityMappingService).extractStringValues(
            AuthorityPrefix.ROLE,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authorityMappingService)
    }
}
