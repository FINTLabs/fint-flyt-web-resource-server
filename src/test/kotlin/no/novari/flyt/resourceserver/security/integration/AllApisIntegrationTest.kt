package no.novari.flyt.resourceserver.security.integration

import no.novari.flyt.resourceserver.security.integration.parameters.TestParameters
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.MethodSources
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("internal-user-api", "internal-client-api", "external-client-api")
class AllApisIntegrationTest : AbstractIntegrationTest() {

    @ParameterizedTest
    @MethodSources(
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#internalApiEnabled"),
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#internalClientApiEnabled"),
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#externalClientApiEnabled"),
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#actuator"),
        MethodSource("no.novari.flyt.resourceserver.security.integration.parameters.TestParametersSource#global")
    )
    fun test(testParameters: TestParameters) {
        performIntegrationTest(testParameters)
    }
}
