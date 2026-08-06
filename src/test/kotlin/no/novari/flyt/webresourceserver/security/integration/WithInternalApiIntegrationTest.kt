package no.novari.flyt.webresourceserver.security.integration

import no.novari.flyt.webresourceserver.security.integration.parameters.TestParameters
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersSource
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.test.context.ActiveProfiles
import java.util.stream.Stream

@ActiveProfiles("internal-user-api")
class WithInternalApiIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `does not register the removed user permission kafka listener`() {
        assertThat(applicationContext.containsBean("userPermissionCachingListener")).isFalse()
    }

    @Test
    fun `registers user authorization service when an OAuth2 client registration is configured`() {
        assertThat(applicationContext.getBeansOfType(ClientRegistrationRepository::class.java)).isNotEmpty()
        assertThat(applicationContext.getBeansOfType(OAuth2AuthorizedClientService::class.java)).isNotEmpty()
        assertThat(applicationContext.getBeansOfType(OAuth2AuthorizedClientManager::class.java)).isNotEmpty()
        assertThat(applicationContext.getBean(UserAuthorizationService::class.java)).isNotNull()
    }

    @ParameterizedTest
    @MethodSource("parameters")
    fun test(testParameters: TestParameters) {
        performIntegrationTest(testParameters)
    }

    companion object {
        @JvmStatic
        fun parameters(): Stream<TestParameters> {
            return Stream
                .of(
                    TestParametersSource.internalApiEnabled(),
                    TestParametersSource.internalClientApiDisabled(),
                    TestParametersSource.externalClientApiDisabled(),
                    TestParametersSource.actuator(),
                    TestParametersSource.global(),
                ).flatMap { it }
        }
    }
}
