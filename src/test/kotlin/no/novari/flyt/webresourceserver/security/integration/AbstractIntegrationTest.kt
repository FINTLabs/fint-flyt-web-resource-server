package no.novari.flyt.webresourceserver.security.integration

import no.novari.cache.FintCache
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorization
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import no.novari.flyt.webresourceserver.security.integration.parameters.ExpectedResult
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParameters
import no.novari.flyt.webresourceserver.security.integration.values.ClientId
import no.novari.flyt.webresourceserver.security.integration.values.PersonalTokenObjectIdentifier
import no.novari.flyt.webresourceserver.security.user.permission.UserPermission
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.Optional
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {
    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @MockitoBean
    private var sourceApplicationAuthorizationRequestService: SourceApplicationAuthorizationRequestService? = null

    @field:MockitoBean(name = "userPermissionCachingListener")
    private var userPermissionCachingListener: ConcurrentMessageListenerContainer<String, UserPermission>? = null

    @MockitoBean
    private var userPermissionCache: FintCache<UUID, UserPermission>? = null

    @BeforeEach
    fun setUp() {
        userPermissionCache?.let {
            mockUserPermission(
                PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
                setOf(1L, 2L),
            )
            mockUserPermission(
                PersonalTokenObjectIdentifier.WITH_NO_SA_AUTHORIZATIONS,
                emptySet(),
            )
        }

        sourceApplicationAuthorizationRequestService?.let {
            mockExternalClientSourceApplicationAuthorizations(
                ClientId.WITH_EXTERNAL_CLIENT_SA_AUTHORIZATION_ID_1,
                authorized = true,
                sourceApplicationId = 1L,
            )
            mockExternalClientSourceApplicationAuthorizations(
                ClientId.WITH_NO_EXTERNAL_CLIENT_SA_AUTHORIZATION,
                authorized = false,
                sourceApplicationId = null,
            )
        }
    }

    private fun mockUserPermission(
        objectIdentifier: PersonalTokenObjectIdentifier,
        sourceApplicationIds: Set<Long>,
    ) {
        Mockito
            .`when`(userPermissionCache!!.getOptional(objectIdentifier.uuid))
            .thenReturn(
                Optional.of(
                    UserPermission(
                        objectIdentifier.uuid,
                        sourceApplicationIds,
                    ),
                ),
            )
    }

    private fun mockExternalClientSourceApplicationAuthorizations(
        clientId: ClientId,
        authorized: Boolean,
        sourceApplicationId: Long?,
    ) {
        Mockito
            .`when`(
                sourceApplicationAuthorizationRequestService!!.getClientAuthorization(clientId.claimValue),
            ).thenReturn(
                Optional.of(
                    SourceApplicationAuthorization(
                        authorized = authorized,
                        clientId = clientId.claimValue,
                        sourceApplicationId = sourceApplicationId,
                    ),
                ),
            )
    }

    protected fun performIntegrationTest(testParameters: TestParameters) {
        val token = testParameters.tokenWrapper.token
        token?.let {
            Mockito.`when`(jwtDecoder.decode(it.tokenValue)).thenReturn(it)
        }

        val expectedResult: ExpectedResult = testParameters.expectedResult

        val headers = HttpHeaders()
        token?.let { headers.setBearerAuth(it.tokenValue) }

        val response =
            testRestTemplate.exchange(
                testParameters.path,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                object : ParameterizedTypeReference<Set<String>>() {},
            )

        assertThat(response.statusCode).isEqualTo(expectedResult.status)
        val result = response.body

        expectedResult.authorities?.let { authorities ->
            assertThat(result).containsExactlyInAnyOrderElementsOf(authorities)
        }
    }
}
