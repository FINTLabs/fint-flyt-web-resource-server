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
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import java.util.Optional

class SourceApplicationJwtConverterTest {
    private lateinit var sourceApplicationAuthorizationRequestService: SourceApplicationAuthorizationRequestService
    private lateinit var sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService
    private lateinit var sourceApplicationJwtConverter: SourceApplicationJwtConverter

    @BeforeEach
    fun setUp() {
        sourceApplicationAuthorizationRequestService =
            mock(SourceApplicationAuthorizationRequestService::class.java)
        sourceApplicationAuthorityMappingService =
            mock(SourceApplicationAuthorityMappingService::class.java)
        sourceApplicationJwtConverter =
            SourceApplicationJwtConverter(
                sourceApplicationAuthorizationRequestService,
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
            sourceApplicationAuthorizationRequestService,
            sourceApplicationAuthorityMappingService,
        )
    }

    @Test
    fun `token with subject but no authorization should throw`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.subject).thenReturn("subjectValue")
        `when`(
            sourceApplicationAuthorizationRequestService.getClientAuthorization("subjectValue"),
        ).thenReturn(Optional.empty())

        assertThatThrownBy { sourceApplicationJwtConverter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)

        verify(sourceApplicationAuthorizationRequestService).getClientAuthorization("subjectValue")
        verifyNoMoreInteractions(
            sourceApplicationAuthorizationRequestService,
            sourceApplicationAuthorityMappingService,
        )
    }

    @Test
    fun `token with subject and authorization should add mapped authority`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.subject).thenReturn("subjectValue")

        val sourceApplicationAuthorization = mock(SourceApplicationAuthorization::class.java)
        `when`(sourceApplicationAuthorization.authorized).thenReturn(true)
        `when`(sourceApplicationAuthorization.sourceApplicationId).thenReturn(3L)
        `when`(
            sourceApplicationAuthorizationRequestService.getClientAuthorization("subjectValue"),
        ).thenReturn(Optional.of(sourceApplicationAuthorization))

        val grantedAuthority = mock(GrantedAuthority::class.java)
        `when`(sourceApplicationAuthorityMappingService.createSourceApplicationAuthority(3L))
            .thenReturn(grantedAuthority)

        val authentication = sourceApplicationJwtConverter.convert(jwt)
        assertThat(authentication.authorities).containsExactly(grantedAuthority)

        verify(sourceApplicationAuthorizationRequestService).getClientAuthorization("subjectValue")
        verify(sourceApplicationAuthorityMappingService).createSourceApplicationAuthority(3L)
        verifyNoMoreInteractions(
            sourceApplicationAuthorizationRequestService,
            sourceApplicationAuthorityMappingService,
        )
    }
}
