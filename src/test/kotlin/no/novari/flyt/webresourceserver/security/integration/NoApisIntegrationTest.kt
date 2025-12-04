package no.novari.flyt.webresourceserver.security.integration

import no.novari.flyt.webresourceserver.security.integration.parameters.TestParameters
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersSource
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class NoApisIntegrationTest : AbstractIntegrationTest() {
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
                    TestParametersSource.internalClientApiDisabled(),
                    TestParametersSource.externalClientApiDisabled(),
                    TestParametersSource.actuator(),
                    TestParametersSource.global(),
                ).flatMap { it }
        }
    }
}
