package no.novari.flyt.webresourceserver.security.integration

import no.novari.flyt.webresourceserver.security.integration.parameters.TestParameters
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersSource
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import java.util.stream.Stream

@ActiveProfiles("internal-user-api")
class WithInternalApiIntegrationTest : AbstractIntegrationTest() {
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
