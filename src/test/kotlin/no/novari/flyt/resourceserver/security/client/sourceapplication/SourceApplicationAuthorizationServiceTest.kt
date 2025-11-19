package no.novari.flyt.resourceserver.security.client.sourceapplication

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import no.novari.flyt.resourceserver.security.client.sourceapplication.exceptions.MultipleSourceApplicationIdsException
import no.novari.flyt.resourceserver.security.client.sourceapplication.exceptions.NoSourceApplicationIdException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class SourceApplicationAuthorizationServiceTest {
    private lateinit var authorityMappingService: AuthorityMappingService
    private lateinit var service: SourceApplicationAuthorizationService

    @BeforeEach
    fun setup() {
        authorityMappingService = Mockito.mock(AuthorityMappingService::class.java)
        service = SourceApplicationAuthorizationService(authorityMappingService)
    }

    @Test
    fun `given single authority should return id`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)

        Mockito
            .`when`(
                authorityMappingService.extractLongValues(
                    AuthorityPrefix.SOURCE_APPLICATION_ID,
                    authorities,
                ),
            ).thenReturn(setOf(1L))

        assertThat(service.getSourceApplicationId(authentication)).isEqualTo(1L)

        Mockito.verify(authentication).authorities
        Mockito.verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authentication, authorityMappingService)
    }

    @Test
    fun `given no authority should throw`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)

        Mockito
            .`when`(
                authorityMappingService.extractLongValues(
                    AuthorityPrefix.SOURCE_APPLICATION_ID,
                    authorities,
                ),
            ).thenReturn(emptySet())

        org.junit.jupiter.api.Assertions.assertThrows(NoSourceApplicationIdException::class.java) {
            service.getSourceApplicationId(authentication)
        }

        Mockito.verify(authentication).authorities
        Mockito.verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authentication, authorityMappingService)
    }

    @Test
    fun `given multiple authorities should throw`() {
        val authentication = Mockito.mock(Authentication::class.java)
        val authorities = Mockito.mock(Collection::class.java) as Collection<GrantedAuthority>
        Mockito.`when`(authentication.authorities).thenReturn(authorities)

        Mockito
            .`when`(
                authorityMappingService.extractLongValues(
                    AuthorityPrefix.SOURCE_APPLICATION_ID,
                    authorities,
                ),
            ).thenReturn(setOf(1L, 2L))

        org.junit.jupiter.api.Assertions.assertThrows(MultipleSourceApplicationIdsException::class.java) {
            service.getSourceApplicationId(authentication)
        }

        Mockito.verify(authentication).authorities
        Mockito.verify(authorityMappingService).extractLongValues(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            authorities,
        )
        Mockito.verifyNoMoreInteractions(authentication, authorityMappingService)
    }
}
