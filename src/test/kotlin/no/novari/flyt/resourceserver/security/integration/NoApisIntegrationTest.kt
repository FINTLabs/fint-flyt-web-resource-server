package no.novari.flyt.resourceserver.security.integration

import no.novari.flyt.resourceserver.security.integration.parameters.TestParameters
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.MethodSources

class NoApisIntegrationTest : AbstractIntegrationTest() {

    @ParameterizedTest
    @MethodSources(
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#internalApiDisabled"),
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#internalClientApiDisabled"),
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#externalClientApiDisabled"),
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#actuator"),
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#global")
    )
    fun test(testParameters: TestParameters) {
        performIntegrationTest(testParameters)
    }
}
