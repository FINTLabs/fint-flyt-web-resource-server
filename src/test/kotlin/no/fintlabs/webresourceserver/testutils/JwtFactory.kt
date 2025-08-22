package no.fintlabs.webresourceserver.testutils

import org.springframework.security.oauth2.jwt.Jwt

object JwtFactory {
    private const val HDR_KID = "kid"
    private const val HDR_TYP = "typ"
    private const val HDR_ALG = "alg"

    private const val CLAIM_SURNAME = "surname"
    private const val CLAIM_GIVENNAME = "givenname"
    private const val CLAIM_EMAIL = "email"
    private const val CLAIM_STUDENTNUMBER = "studentnumber"
    private const val CLAIM_EMPLOYEE_ID = "employeeId"
    private const val CLAIM_ORGANIZATION_ID = "organizationid"
    private const val CLAIM_ORGANIZATION_NUMBER = "organizationnumber"
    private const val CLAIM_ROLES = "roles"

    fun createEndUserJwt(): Jwt {
        return Jwt
            .withTokenValue("test")
            .header(HDR_KID, "123456789")
            .header(HDR_TYP, "JWT")
            .header(HDR_ALG, "RS256")
            .claims { claims ->
                claims.putAll(
                    mapOf(
                        CLAIM_SURNAME to "Testesen",
                        CLAIM_GIVENNAME to "Test",
                        CLAIM_EMAIL to "test@test.com",
                        CLAIM_STUDENTNUMBER to "123456",
                        CLAIM_EMPLOYEE_ID to "654321",
                        CLAIM_ORGANIZATION_ID to "\"test.com\"",
                        CLAIM_ORGANIZATION_NUMBER to "\"123456789\"",
                        CLAIM_ROLES to listOf("role1", "role2", "role3"),
                    ),
                )
            }.build()
    }
}
