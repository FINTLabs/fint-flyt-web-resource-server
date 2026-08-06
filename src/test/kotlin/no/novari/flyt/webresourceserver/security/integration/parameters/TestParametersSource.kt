package no.novari.flyt.webresourceserver.security.integration.parameters

import no.novari.flyt.webresourceserver.UrlPaths
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.ACTUATOR
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.EXTERNAL_CLIENT_API_IF_DISABLED
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.EXTERNAL_CLIENT_API_IF_ENABLED
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.GLOBAL
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.INTERNAL_CLIENT_API_IF_DISABLED
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.INTERNAL_CLIENT_API_IF_ENABLED
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_DISABLED
import no.novari.flyt.webresourceserver.security.integration.parameters.TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED
import no.novari.flyt.webresourceserver.security.integration.values.ClientId
import no.novari.flyt.webresourceserver.security.integration.values.PersonalTokenObjectIdentifier
import no.novari.flyt.webresourceserver.security.integration.values.PersonalTokenOrgId
import no.novari.flyt.webresourceserver.security.user.UserRole
import org.springframework.http.HttpStatus
import java.util.stream.Stream

object TestParametersSource {
    @JvmStatic
    fun internalApiEnabled(): Stream<TestParameters> {
        return Stream.concat(
            streamFor(INTERNAL_USER_API_IF_INTERNAL_API_ENABLED),
            streamFor(INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED),
        )
    }

    @JvmStatic
    fun internalApiDisabled(): Stream<TestParameters> {
        return Stream.concat(
            streamFor(INTERNAL_USER_API_IF_INTERNAL_API_DISABLED),
            streamFor(TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_DISABLED),
        )
    }

    @JvmStatic
    fun internalClientApiEnabled(): Stream<TestParameters> {
        return streamFor(INTERNAL_CLIENT_API_IF_ENABLED)
    }

    @JvmStatic
    fun internalClientApiDisabled(): Stream<TestParameters> {
        return streamFor(INTERNAL_CLIENT_API_IF_DISABLED)
    }

    @JvmStatic
    fun externalClientApiEnabled(): Stream<TestParameters> {
        return streamFor(EXTERNAL_CLIENT_API_IF_ENABLED)
    }

    @JvmStatic
    fun externalClientApiDisabled(): Stream<TestParameters> {
        return streamFor(EXTERNAL_CLIENT_API_IF_DISABLED)
    }

    @JvmStatic
    fun actuator(): Stream<TestParameters> {
        return streamFor(ACTUATOR)
    }

    @JvmStatic
    fun global(): Stream<TestParameters> {
        return streamFor(GLOBAL)
    }

    private fun streamFor(category: TestParametersCategory): Stream<TestParameters> {
        return testParametersPerApiAndToggleStatus.getValue(category).stream()
    }

    private val testParametersPerApiAndToggleStatus: Map<TestParametersCategory, List<TestParameters>> =
        createTokensWithExpectedResultPerApiAndToggleStatus()
            .flatMap { (tokenWrapper, expectedResolver) ->
                TestParametersCategory.entries.map { category ->
                    category to
                        TestParameters(
                            getPathForApi(category),
                            tokenWrapper,
                            expectedResolver(category),
                        )
                }
            }.groupBy({ it.first }, { it.second })

    private fun getPathForApi(category: TestParametersCategory): String {
        return when (category) {
            INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
            INTERNAL_USER_API_IF_INTERNAL_API_DISABLED,
            -> "${UrlPaths.INTERNAL_API}/dummy"

            INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
            TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_DISABLED,
            -> "${UrlPaths.INTERNAL_ADMIN_API}/dummy"

            INTERNAL_CLIENT_API_IF_ENABLED,
            INTERNAL_CLIENT_API_IF_DISABLED,
            -> "${UrlPaths.INTERNAL_CLIENT_API}/dummy"

            EXTERNAL_CLIENT_API_IF_ENABLED,
            EXTERNAL_CLIENT_API_IF_DISABLED,
            -> "${UrlPaths.EXTERNAL_API}/dummy"

            GLOBAL -> "/not/matching/any/filter"

            ACTUATOR -> "/actuator/dummy"
        }
    }

    private fun createTokensWithExpectedResultPerApiAndToggleStatus():
        List<Pair<TokenWrapper, (TestParametersCategory) -> ExpectedResult>> =
        listOf(
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_NO_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
                setOf(UserRole.USER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> ExpectedResult(HttpStatus.FORBIDDEN)

                    ACTUATOR -> ExpectedResult(HttpStatus.OK)

                    else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
                setOf(UserRole.USER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER"),
                        )
                    }

                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> {
                        ExpectedResult(HttpStatus.FORBIDDEN)
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_SA_AUTHORIZATIONS,
                setOf(UserRole.USER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER"),
                        )
                    }

                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> {
                        ExpectedResult(HttpStatus.FORBIDDEN)
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
                setOf(UserRole.USER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER"),
                        )
                    }

                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> {
                        ExpectedResult(HttpStatus.FORBIDDEN)
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ADMIN_AND_DEV_ACCESS,
                PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
                setOf(UserRole.USER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> ExpectedResult(HttpStatus.FORBIDDEN)

                    ACTUATOR -> ExpectedResult(HttpStatus.OK)

                    else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_NO_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
                setOf(UserRole.ADMIN),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> ExpectedResult(HttpStatus.FORBIDDEN)

                    ACTUATOR -> ExpectedResult(HttpStatus.OK)

                    else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
                setOf(UserRole.ADMIN),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER", "ROLE_ADMIN"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_SA_AUTHORIZATIONS,
                setOf(UserRole.ADMIN),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER", "ROLE_ADMIN"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
                setOf(UserRole.ADMIN),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER", "ROLE_ADMIN"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ADMIN_AND_DEV_ACCESS,
                PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
                setOf(UserRole.ADMIN),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER", "ROLE_ADMIN"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_NO_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
                setOf(UserRole.DEVELOPER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> ExpectedResult(HttpStatus.FORBIDDEN)

                    ACTUATOR -> ExpectedResult(HttpStatus.OK)

                    else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
                setOf(UserRole.DEVELOPER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER", "ROLE_ADMIN", "ROLE_DEVELOPER"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_NO_SA_AUTHORIZATIONS,
                setOf(UserRole.DEVELOPER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER", "ROLE_ADMIN", "ROLE_DEVELOPER"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
                PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
                setOf(UserRole.DEVELOPER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER", "ROLE_ADMIN", "ROLE_DEVELOPER"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createPersonalToken(
                PersonalTokenOrgId.WITH_ADMIN_AND_DEV_ACCESS,
                PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
                setOf(UserRole.DEVELOPER),
            ) to { category ->
                when (category) {
                    INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                    INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
                    -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("ROLE_USER", "ROLE_ADMIN", "ROLE_DEVELOPER"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createClientToken(ClientId.AUTHORIZED_FOR_INTERNAL_CLIENT_API) to { category ->
                if (
                    category == INTERNAL_CLIENT_API_IF_ENABLED ||
                    category == ACTUATOR
                ) {
                    ExpectedResult(HttpStatus.OK)
                } else {
                    ExpectedResult(HttpStatus.UNAUTHORIZED)
                }
            },
            TokenFactory.createClientToken(ClientId.NOT_AUTHORIZED_FOR_INTERNAL_CLIENT_API) to { category ->
                when (category) {
                    INTERNAL_CLIENT_API_IF_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)
                    ACTUATOR -> ExpectedResult(HttpStatus.OK)
                    else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
                }
            },
            TokenFactory.createClientToken(ClientId.WITH_EXTERNAL_CLIENT_SA_AUTHORIZATION_ID_1) to { category ->
                when (category) {
                    INTERNAL_CLIENT_API_IF_ENABLED -> {
                        ExpectedResult(HttpStatus.FORBIDDEN)
                    }

                    EXTERNAL_CLIENT_API_IF_ENABLED -> {
                        ExpectedResult(
                            HttpStatus.OK,
                            setOf("SOURCE_APPLICATION_ID_1"),
                        )
                    }

                    ACTUATOR -> {
                        ExpectedResult(HttpStatus.OK)
                    }

                    else -> {
                        ExpectedResult(HttpStatus.UNAUTHORIZED)
                    }
                }
            },
            TokenFactory.createClientToken(ClientId.WITH_NO_EXTERNAL_CLIENT_SA_AUTHORIZATION) to { category ->
                when (category) {
                    INTERNAL_CLIENT_API_IF_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)
                    ACTUATOR -> ExpectedResult(HttpStatus.OK)
                    else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
                }
            },
            TokenWrapper.none() to { category ->
                when (category) {
                    ACTUATOR -> ExpectedResult(HttpStatus.OK)
                    else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
                }
            },
        )
}
