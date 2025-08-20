package no.fintlabs.webresourceserver.testutils

import org.springframework.security.oauth2.jwt.Jwt

object JwtFactory {
    fun createEndUserJwt(): Jwt {
        return Jwt
            .withTokenValue("test")
            .header("kid", "123456789")
            .header("typ", "JWT")
            .header("alg", "RS256")
            .claims { claims ->
                claims["surname"] = "Testesen"
                claims["givenname"] = "Test"
                claims["email"] = "test@test.com"
                claims["studentnumber"] = "123456"
                claims["employeeId"] = "654321"
                claims["organizationid"] = "\"test.com\""
                claims["organizationnumber"] = "\"123456789\""
                claims["roles"] = listOf("role1", "role2", "role3")
            }.build()
    }
}
