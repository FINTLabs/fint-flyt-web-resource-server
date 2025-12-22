package no.novari.flyt.webresourceserver.security.client.sourceapplication

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
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
            Mockito.mock(SourceApplicationAuthorizationRequestService::class.java)
        sourceApplicationAuthorityMappingService =
            Mockito.mock(SourceApplicationAuthorityMappingService::class.java)
        sourceApplicationJwtConverter =
            SourceApplicationJwtConverter(
                sourceApplicationAuthorizationRequestService,
                sourceApplicationAuthorityMappingService,
            )
    }

    @Test
    fun `token without subject should throw`() {
        val jwt = Mockito.mock(Jwt::class.java)
        Mockito.`when`(jwt.subject).thenReturn(null)

        assertThatThrownBy { sourceApplicationJwtConverter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)

        Mockito.verifyNoInteractions(
            sourceApplicationAuthorizationRequestService,
            sourceApplicationAuthorityMappingService,
        )
    }

    @Test
    fun `token with subject but no authorization should throw`() {
        val jwt = Mockito.mock(Jwt::class.java)
        Mockito.`when`(jwt.subject).thenReturn("subjectValue")
        Mockito
            .`when`(
                sourceApplicationAuthorizationRequestService.getClientAuthorization("subjectValue"),
            ).thenReturn(Optional.empty())

        assertThatThrownBy { sourceApplicationJwtConverter.convert(jwt) }
            .isInstanceOf(BadCredentialsException::class.java)

        Mockito.verify(sourceApplicationAuthorizationRequestService).getClientAuthorization("subjectValue")
        Mockito.verifyNoMoreInteractions(
            sourceApplicationAuthorizationRequestService,
            sourceApplicationAuthorityMappingService,
        )
    }

    @Test
    fun `token with subject and authorization should add mapped authority`() {
        val jwt = Mockito.mock(Jwt::class.java)
        Mockito.`when`(jwt.subject).thenReturn("subjectValue")

        val sourceApplicationAuthorization = Mockito.mock(SourceApplicationAuthorization::class.java)
        Mockito.`when`(sourceApplicationAuthorization.authorized).thenReturn(true)
        Mockito.`when`(sourceApplicationAuthorization.sourceApplicationId).thenReturn(3L)
        Mockito
            .`when`(
                sourceApplicationAuthorizationRequestService.getClientAuthorization("subjectValue"),
            ).thenReturn(Optional.of(sourceApplicationAuthorization))

        val grantedAuthority = Mockito.mock(GrantedAuthority::class.java)
        Mockito
            .`when`(sourceApplicationAuthorityMappingService.createSourceApplicationAuthority(3L))
            .thenReturn(grantedAuthority)

        val authentication = sourceApplicationJwtConverter.convert(jwt)
        assertThat(authentication.authorities).containsExactly(grantedAuthority)

        Mockito.verify(sourceApplicationAuthorizationRequestService).getClientAuthorization("subjectValue")
        Mockito.verify(sourceApplicationAuthorityMappingService).createSourceApplicationAuthority(3L)
        Mockito.verifyNoMoreInteractions(
            sourceApplicationAuthorizationRequestService,
            sourceApplicationAuthorityMappingService,
        )
    }
}
