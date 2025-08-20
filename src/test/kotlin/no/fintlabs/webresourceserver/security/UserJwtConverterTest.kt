package no.fintlabs.webresourceserver.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.fintlabs.cache.FintCache
import no.fintlabs.webresourceserver.security.properties.InternalApiSecurityProperties
import no.fintlabs.webresourceserver.security.user.UserClaimFormattingService
import no.fintlabs.webresourceserver.security.user.UserJwtConverter
import no.fintlabs.webresourceserver.security.user.userpermission.UserPermission
import no.fintlabs.webresourceserver.testutils.JwtFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class UserJwtConverterTest {
    private val dummyCache = mockk<FintCache<String, UserPermission>>(relaxed = true)

    private val userClaimFormattingService = spyk(UserClaimFormattingService(dummyCache))

    private val properties =
        mockk<InternalApiSecurityProperties>(relaxed = true).apply {
            every { adminRole } returns ""
        }

    private val converter = UserJwtConverter(properties, userClaimFormattingService)

    @Test
    fun convertingFintUserJwtShouldResultInThreeAuthorities() {
        val jwt: Jwt = JwtFactory.createEndUserJwt()

        val authenticationToken = converter.convert(jwt) as JwtAuthenticationToken

        assertEquals(3, authenticationToken.authorities.size)
    }

    @Test
    fun convertingFintFlytUserJwtShouldRemoveIllegalCharactersFromClaims() {
        val jwt = JwtFactory.createEndUserJwt()

        val authToken = converter.convert(jwt) as JwtAuthenticationToken
        val modifiedJwt = authToken.token

        val orgId = modifiedJwt.getClaimAsString("organizationid")
        val orgNumber = modifiedJwt.getClaimAsString("organizationnumber")
        assertEquals("test.com", orgId)
        assertEquals("123456789", orgNumber)
    }
}
