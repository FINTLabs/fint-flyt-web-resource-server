package no.novari.flyt.resourceserver.security.client.sourceapplication

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.core.authority.SimpleGrantedAuthority

class SourceApplicationAuthorityMappingServiceTest {
    private val authorityMappingService = Mockito.mock(AuthorityMappingService::class.java)
    private val service = SourceApplicationAuthorityMappingService(authorityMappingService)

    @Test
    fun `given id should create granted authority`() {
        Mockito.`when`(
            authorityMappingService.toAuthority(
                AuthorityPrefix.SOURCE_APPLICATION_ID,
                "2",
            ),
        ).thenReturn("SOURCE_APPLICATION_ID_2")

        val authority = service.createSourceApplicationAuthority(2L)

        assertThat(authority).isEqualTo(SimpleGrantedAuthority("SOURCE_APPLICATION_ID_2"))
    }
}
