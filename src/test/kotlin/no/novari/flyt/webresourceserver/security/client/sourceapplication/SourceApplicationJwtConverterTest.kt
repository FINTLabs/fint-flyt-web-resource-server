package no.novari.flyt.webresourceserver.security.client.sourceapplication

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.client.HttpServerErrorException

class SourceApplicationJwtConverterTest {
    private lateinit var sourceApplicationAuthorizationClient: SourceApplicationAuthorizationClient
    private lateinit var sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService
    private lateinit var sourceApplicationJwtConverter: SourceApplicationJwtConverter

    @BeforeEach
    fun setUp() {
        sourceApplicationAuthorizationClient =
            mock(SourceApplicationAuthorizationClient::class.java)
        sourceApplicationAuthorityMappingService =
            mock(SourceApplicationAuthorityMappingService::class.java)
        sourceApplicationJwtConverter =
            SourceApplicationJwtConverter(
                sourceApplicationAuthorizationClient,
                sourceApplicationAuthorityMappingService,
            )
    }

    @Test
    fun `token without subject should throw`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.subject).thenReturn(null)

        assertThatThrownBy { sourceApplicationJwtConverter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)

        verifyNoInteractions(
            sourceApplicationAuthorizationClient,
            sourceApplicationAuthorityMappingService,
        )
    }

    @Test
    fun `token with subject but no authorization should throw`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.subject).thenReturn("subjectValue")
        `when`(
            sourceApplicationAuthorizationClient.getClientAuthorization("subjectValue"),
        ).thenReturn(null)

        assertThatThrownBy { sourceApplicationJwtConverter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)

        verify(sourceApplicationAuthorizationClient).getClientAuthorization("subjectValue")
        verifyNoMoreInteractions(
            sourceApplicationAuthorizationClient,
            sourceApplicationAuthorityMappingService,
        )
    }

    @Test
    fun `failing lookup should not be reported as bad credentials`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.subject).thenReturn("subjectValue")
        `when`(sourceApplicationAuthorizationClient.getClientAuthorization("subjectValue"))
            .thenThrow(HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))

        assertThatThrownBy { sourceApplicationJwtConverter.convert(jwt) }
            .isInstanceOf(HttpServerErrorException::class.java)
    }

    @Test
    fun `token with subject and authorization should add mapped authority`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.subject).thenReturn("subjectValue")

        val sourceApplicationAuthorization = mock(SourceApplicationAuthorization::class.java)
        `when`(sourceApplicationAuthorization.authorized).thenReturn(true)
        `when`(sourceApplicationAuthorization.sourceApplicationId).thenReturn(3L)
        `when`(
            sourceApplicationAuthorizationClient.getClientAuthorization("subjectValue"),
        ).thenReturn(sourceApplicationAuthorization)

        val grantedAuthority = mock(GrantedAuthority::class.java)
        `when`(sourceApplicationAuthorityMappingService.createSourceApplicationAuthority(3L))
            .thenReturn(grantedAuthority)

        val authentication = sourceApplicationJwtConverter.convert(jwt)
        assertThat(authentication.authorities).containsExactly(grantedAuthority)

        verify(sourceApplicationAuthorizationClient).getClientAuthorization("subjectValue")
        verify(sourceApplicationAuthorityMappingService).createSourceApplicationAuthority(3L)
        verifyNoMoreInteractions(
            sourceApplicationAuthorizationClient,
            sourceApplicationAuthorityMappingService,
        )
    }
}
