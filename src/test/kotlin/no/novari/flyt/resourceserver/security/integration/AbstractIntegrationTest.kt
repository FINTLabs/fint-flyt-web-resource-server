package no.novari.flyt.resourceserver.security.integration

import java.util.Optional
import java.util.UUID
import no.novari.cache.FintCache
import no.novari.flyt.resourceserver.security.client.sourceapplication.SourceApplicationAuthorization
import no.novari.flyt.resourceserver.security.client.sourceapplication.SourceApplicationAuthorizationRequestService
import no.novari.flyt.resourceserver.security.integration.parameters.ExpectedResult
import no.novari.flyt.resourceserver.security.integration.parameters.TestParameters
import no.novari.flyt.resourceserver.security.integration.values.ClientId
import no.novari.flyt.resourceserver.security.integration.values.PersonalTokenObjectIdentifier
import no.novari.flyt.resourceserver.security.user.permission.UserPermission
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
abstract class AbstractIntegrationTest {

    @MockitoBean
    private lateinit var reactiveJwtDecoder: ReactiveJwtDecoder

    @Autowired
    private lateinit var webTestClient: WebTestClient

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
                setOf(1L, 2L)
            )
            mockUserPermission(
                PersonalTokenObjectIdentifier.WITH_NO_SA_AUTHORIZATIONS,
                emptySet()
            )
        }

        sourceApplicationAuthorizationRequestService?.let {
            mockExternalClientSourceApplicationAuthorizations(
                ClientId.WITH_EXTERNAL_CLIENT_SA_AUTHORIZATION_ID_1,
                authorized = true,
                sourceApplicationId = 1L
            )
            mockExternalClientSourceApplicationAuthorizations(
                ClientId.WITH_NO_EXTERNAL_CLIENT_SA_AUTHORIZATION,
                authorized = false,
                sourceApplicationId = null
            )
        }
    }

    private fun mockUserPermission(
        objectIdentifier: PersonalTokenObjectIdentifier,
        sourceApplicationIds: Set<Long>
    ) {
        Mockito.`when`(userPermissionCache!!.getOptional(objectIdentifier.uuid))
            .thenReturn(
                Optional.of(
                    UserPermission(
                        objectIdentifier.uuid,
                        sourceApplicationIds
                    )
                )
            )
    }

    private fun mockExternalClientSourceApplicationAuthorizations(
        clientId: ClientId,
        authorized: Boolean,
        sourceApplicationId: Long?
    ) {
        Mockito.`when`(
            sourceApplicationAuthorizationRequestService!!.getClientAuthorization(clientId.claimValue)
        ).thenReturn(
            Optional.of(
                SourceApplicationAuthorization(
                    authorized = authorized,
                    clientId = clientId.claimValue,
                    sourceApplicationId = sourceApplicationId
                )
            )
        )
    }

    protected fun performIntegrationTest(testParameters: TestParameters) {
        val token = testParameters.tokenWrapper.token
        token?.let {
            Mockito.`when`(reactiveJwtDecoder.decode(it.tokenValue)).thenReturn(Mono.just(it))
        }

        val expectedResult: ExpectedResult = testParameters.expectedResult

        val result = webTestClient
            .get()
            .uri(testParameters.path)
            .headers { headers ->
                token?.let { headers.setBearerAuth(it.tokenValue) }
            }
            .exchange()
            .expectStatus().isEqualTo(expectedResult.status)
            .returnResult(object : ParameterizedTypeReference<Set<String>>() {})
            .responseBody
            .blockFirst()

        expectedResult.authorities?.let { authorities ->
            assertThat(result).containsExactlyInAnyOrderElementsOf(authorities)
        }
    }
}
