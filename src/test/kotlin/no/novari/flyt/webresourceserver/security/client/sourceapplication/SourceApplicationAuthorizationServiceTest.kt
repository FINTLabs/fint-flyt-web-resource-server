package no.novari.flyt.webresourceserver.security.client.sourceapplication

import no.novari.flyt.webresourceserver.security.AuthorityMappingService
import no.novari.flyt.webresourceserver.security.AuthorityPrefix
import no.novari.flyt.webresourceserver.security.client.sourceapplication.exceptions.MultipleSourceApplicationIdsException
import no.novari.flyt.webresourceserver.security.client.sourceapplication.exceptions.NoSourceApplicationIdException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class SourceApplicationAuthorizationServiceTest {
    private lateinit var authorityMappingService: AuthorityMappingService
    private lateinit var service: SourceApplicationAuthorizationService

    @BeforeEach
    fun setup() {
        authorityMappingService = mock(AuthorityMappingService::class.java)
        service = SourceApplicationAuthorizationService(authorityMappingService)
    }

    @Test
    fun `given single authority should return id`() {
        val authentication = mock(Authentication::class.java)
        val authorities = mock(Collection::class.java) as Collection<GrantedAuthority>
        `when`(authentication.authorities).thenReturn(authorities)

        `when`(
            authorityMappingService.extractLongValues(
                AuthorityPrefix.SOURCE_APPLICATION_ID,
                authorities,
            ),
        ).thenReturn(setOf(1L))

        assertThat(service.getSourceApplicationId(authentication)).isEqualTo(1L)

        verify(authentication).authorities
        verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        verifyNoMoreInteractions(authentication, authorityMappingService)
    }

    @Test
    fun `given no authority should throw`() {
        val authentication = mock(Authentication::class.java)
        val authorities = mock(Collection::class.java) as Collection<GrantedAuthority>
        `when`(authentication.authorities).thenReturn(authorities)

        `when`(
            authorityMappingService.extractLongValues(
                AuthorityPrefix.SOURCE_APPLICATION_ID,
                authorities,
            ),
        ).thenReturn(emptySet())

        org.junit.jupiter.api.Assertions.assertThrows(NoSourceApplicationIdException::class.java) {
            service.getSourceApplicationId(authentication)
        }

        verify(authentication).authorities
        verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        verifyNoMoreInteractions(authentication, authorityMappingService)
    }

    @Test
    fun `given multiple authorities should throw`() {
        val authentication = mock(Authentication::class.java)
        val authorities = mock(Collection::class.java) as Collection<GrantedAuthority>
        `when`(authentication.authorities).thenReturn(authorities)

        `when`(
            authorityMappingService.extractLongValues(
                AuthorityPrefix.SOURCE_APPLICATION_ID,
                authorities,
            ),
        ).thenReturn(setOf(1L, 2L))

        org.junit.jupiter.api.Assertions.assertThrows(MultipleSourceApplicationIdsException::class.java) {
            service.getSourceApplicationId(authentication)
        }

        verify(authentication).authorities
        verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        verifyNoMoreInteractions(authentication, authorityMappingService)
    }
}
