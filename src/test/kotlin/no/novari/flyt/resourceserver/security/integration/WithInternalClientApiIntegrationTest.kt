package no.novari.flyt.resourceserver.security.integration

import no.novari.flyt.resourceserver.security.integration.parameters.TestParameters
import no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import java.util.stream.Stream

@ActiveProfiles("internal-client-api")
class WithInternalClientApiIntegrationTest : AbstractIntegrationTest() {
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
                    TestParametersSource.internalApiDisabled(),
                    TestParametersSource.internalClientApiEnabled(),
                    TestParametersSource.externalClientApiDisabled(),
                    TestParametersSource.actuator(),
                    TestParametersSource.global(),
                ).flatMap { it }
        }
    }
}
