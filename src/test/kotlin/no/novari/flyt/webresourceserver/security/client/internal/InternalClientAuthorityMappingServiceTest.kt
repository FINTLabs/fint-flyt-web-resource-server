package no.novari.flyt.webresourceserver.security.client.internal

import no.novari.flyt.webresourceserver.security.AuthorityMappingService
import no.novari.flyt.webresourceserver.security.AuthorityPrefix
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class InternalClientAuthorityMappingServiceTest {
    private val authorityMappingService = Mockito.mock(AuthorityMappingService::class.java)
    private lateinit var service: InternalClientAuthorityMappingService

    @BeforeEach
    fun setUp() {
        service = InternalClientAuthorityMappingService(authorityMappingService)
    }

    @Test
    fun `createInternalClientIdAuthority should map value`() {
        Mockito
            .`when`(
                authorityMappingService.toAuthority(AuthorityPrefix.CLIENT_ID, "client-1"),
            ).thenReturn("CLIENT_ID_client-1")

        val authority = service.createInternalClientIdAuthority("client-1")

        assertThat(authority.authority).isEqualTo("CLIENT_ID_client-1")
    }

    @Test
    fun `createInternalClientIdAuthorityStrings should map every id`() {
        Mockito
            .`when`(
                authorityMappingService.toAuthority(AuthorityPrefix.CLIENT_ID, "client-1"),
            ).thenReturn("CLIENT_ID_client-1")
        Mockito
            .`when`(
                authorityMappingService.toAuthority(AuthorityPrefix.CLIENT_ID, "client-2"),
            ).thenReturn("CLIENT_ID_client-2")

        val values = service.createInternalClientIdAuthorityStrings(listOf("client-1", "client-2"))

        assertThat(values).isEqualTo(setOf("CLIENT_ID_client-1", "CLIENT_ID_client-2"))
    }

    @Test
    fun `createInternalClientIdAuthorityString should delegate to mapping service`() {
        Mockito
            .`when`(
                authorityMappingService.toAuthority(AuthorityPrefix.CLIENT_ID, "client-3"),
            ).thenReturn("CLIENT_ID_client-3")

        val value = service.createInternalClientIdAuthorityString("client-3")

        assertThat(value).isEqualTo("CLIENT_ID_client-3")
    }
}
