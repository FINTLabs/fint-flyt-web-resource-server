package no.novari.flyt.resourceserver.security.integration.parameters

import java.util.stream.Stream
import no.novari.flyt.resourceserver.UrlPaths
import no.novari.flyt.resourceserver.security.integration.values.ClientId
import no.novari.flyt.resourceserver.security.integration.values.PersonalTokenObjectIdentifier
import no.novari.flyt.resourceserver.security.integration.values.PersonalTokenOrgId
import no.novari.flyt.resourceserver.security.user.UserRole
import org.springframework.http.HttpStatus

object TestParametersSource {

    @JvmStatic
    fun internalApiEnabled(): Stream<TestParameters> = Stream.concat(
        streamFor(TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED),
        streamFor(TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED)
    )

    @JvmStatic
    fun internalApiDisabled(): Stream<TestParameters> = Stream.concat(
        streamFor(TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_DISABLED),
        streamFor(TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_DISABLED)
    )

    @JvmStatic
    fun internalClientApiEnabled(): Stream<TestParameters> =
        streamFor(TestParametersCategory.INTERNAL_CLIENT_API_IF_ENABLED)

    @JvmStatic
    fun internalClientApiDisabled(): Stream<TestParameters> =
        streamFor(TestParametersCategory.INTERNAL_CLIENT_API_IF_DISABLED)

    @JvmStatic
    fun externalClientApiEnabled(): Stream<TestParameters> =
        streamFor(TestParametersCategory.EXTERNAL_CLIENT_API_IF_ENABLED)

    @JvmStatic
    fun externalClientApiDisabled(): Stream<TestParameters> =
        streamFor(TestParametersCategory.EXTERNAL_CLIENT_API_IF_DISABLED)

    @JvmStatic
    fun actuator(): Stream<TestParameters> = streamFor(TestParametersCategory.ACTUATOR)

    @JvmStatic
    fun global(): Stream<TestParameters> = streamFor(TestParametersCategory.GLOBAL)

    private fun streamFor(category: TestParametersCategory): Stream<TestParameters> =
        testParametersPerApiAndToggleStatus.getValue(category).stream()

    private val testParametersPerApiAndToggleStatus: Map<TestParametersCategory, List<TestParameters>> =
        createTokensWithExpectedResultPerApiAndToggleStatus()
            .flatMap { (tokenWrapper, expectedResolver) ->
                TestParametersCategory.entries.map { category ->
                    category to TestParameters(
                        getPathForApi(category),
                        tokenWrapper,
                        expectedResolver(category)
                    )
                }
            }
            .groupBy({ it.first }, { it.second })

    private fun getPathForApi(category: TestParametersCategory) = when (category) {
        TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
        TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_DISABLED -> "${UrlPaths.INTERNAL_API}/dummy"

        TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED,
        TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_DISABLED -> "${UrlPaths.INTERNAL_ADMIN_API}/dummy"

        TestParametersCategory.INTERNAL_CLIENT_API_IF_ENABLED,
        TestParametersCategory.INTERNAL_CLIENT_API_IF_DISABLED -> "${UrlPaths.INTERNAL_CLIENT_API}/dummy"

        TestParametersCategory.EXTERNAL_CLIENT_API_IF_ENABLED,
        TestParametersCategory.EXTERNAL_CLIENT_API_IF_DISABLED -> "${UrlPaths.EXTERNAL_API}/dummy"

        TestParametersCategory.GLOBAL -> "/not/matching/any/filter"
        TestParametersCategory.ACTUATOR -> "/actuator/dummy"
    }

    private fun createTokensWithExpectedResultPerApiAndToggleStatus():
        List<Pair<TokenWrapper, (TestParametersCategory) -> ExpectedResult>> = listOf(
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_NO_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
            setOf(UserRole.USER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
            setOf(UserRole.USER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf("ROLE_USER")
                )

                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)
                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_SA_AUTHORIZATIONS,
            setOf(UserRole.USER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf("ROLE_USER")
                )

                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)
                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
            setOf(UserRole.USER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf("ROLE_USER", "SOURCE_APPLICATION_ID_1", "SOURCE_APPLICATION_ID_2")
                )

                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)
                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ADMIN_AND_DEV_ACCESS,
            PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
            setOf(UserRole.USER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_NO_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
            setOf(UserRole.ADMIN)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
            setOf(UserRole.ADMIN)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf("ROLE_USER", "ROLE_ADMIN")
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_SA_AUTHORIZATIONS,
            setOf(UserRole.ADMIN)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf("ROLE_USER", "ROLE_ADMIN")
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
            setOf(UserRole.ADMIN)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf(
                        "ROLE_USER",
                        "ROLE_ADMIN",
                        "SOURCE_APPLICATION_ID_1",
                        "SOURCE_APPLICATION_ID_2"
                    )
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ADMIN_AND_DEV_ACCESS,
            PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
            setOf(UserRole.ADMIN)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf(
                        "ROLE_USER",
                        "ROLE_ADMIN",
                        "SOURCE_APPLICATION_ID_1",
                        "SOURCE_APPLICATION_ID_2"
                    )
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_NO_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
            setOf(UserRole.DEVELOPER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_USER_PERMISSION,
            setOf(UserRole.DEVELOPER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf("ROLE_USER", "ROLE_ADMIN", "ROLE_DEVELOPER")
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_NO_SA_AUTHORIZATIONS,
            setOf(UserRole.DEVELOPER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf("ROLE_USER", "ROLE_ADMIN", "ROLE_DEVELOPER")
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ALL_USER_ACCESS,
            PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
            setOf(UserRole.DEVELOPER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf(
                        "ROLE_USER",
                        "ROLE_ADMIN",
                        "ROLE_DEVELOPER",
                        "SOURCE_APPLICATION_ID_1",
                        "SOURCE_APPLICATION_ID_2"
                    )
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createPersonalToken(
            PersonalTokenOrgId.WITH_ADMIN_AND_DEV_ACCESS,
            PersonalTokenObjectIdentifier.WITH_SA_1_2_AUTHORIZATIONS,
            setOf(UserRole.DEVELOPER)
        ) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_USER_API_IF_INTERNAL_API_ENABLED,
                TestParametersCategory.INTERNAL_ADMIN_API_IF_INTERNAL_API_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf(
                        "ROLE_USER",
                        "ROLE_ADMIN",
                        "ROLE_DEVELOPER",
                        "SOURCE_APPLICATION_ID_1",
                        "SOURCE_APPLICATION_ID_2"
                    )
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createClientToken(ClientId.AUTHORIZED_FOR_INTERNAL_CLIENT_API) to { category ->
            if (
                category == TestParametersCategory.INTERNAL_CLIENT_API_IF_ENABLED ||
                category == TestParametersCategory.ACTUATOR
            ) {
                ExpectedResult(HttpStatus.OK)
            } else {
                ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createClientToken(ClientId.NOT_AUTHORIZED_FOR_INTERNAL_CLIENT_API) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_CLIENT_API_IF_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)
                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createClientToken(ClientId.WITH_EXTERNAL_CLIENT_SA_AUTHORIZATION_ID_1) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_CLIENT_API_IF_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)
                TestParametersCategory.EXTERNAL_CLIENT_API_IF_ENABLED -> ExpectedResult(
                    HttpStatus.OK,
                    setOf("SOURCE_APPLICATION_ID_1")
                )

                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenFactory.createClientToken(ClientId.WITH_NO_EXTERNAL_CLIENT_SA_AUTHORIZATION) to { category ->
            when (category) {
                TestParametersCategory.INTERNAL_CLIENT_API_IF_ENABLED -> ExpectedResult(HttpStatus.FORBIDDEN)
                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        },
        TokenWrapper.none() to { category ->
            when (category) {
                TestParametersCategory.ACTUATOR -> ExpectedResult(HttpStatus.OK)
                else -> ExpectedResult(HttpStatus.UNAUTHORIZED)
            }
        }
    )
}
