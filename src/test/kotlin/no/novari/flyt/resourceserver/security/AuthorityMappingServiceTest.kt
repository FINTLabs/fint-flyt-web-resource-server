package no.novari.flyt.resourceserver.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class AuthorityMappingServiceTest {

    private val authorityMappingService = AuthorityMappingService()

    @Test
    fun `given authority prefix and value should return concatenated string`() {
        val prefix = AuthorityPrefix.CLIENT_ID
        val value = "testAuthorityValue"
        val authorityString = authorityMappingService.toAuthority(prefix, value)

        assertThat(authorityString).isEqualTo("CLIENT_ID_testAuthorityValue")
    }

    @Test
    fun `given prefix and authorities should return long values`() {
        val prefix1 = AuthorityPrefix.CLIENT_ID
        val prefix2 = AuthorityPrefix.ROLE

        val authorities: Set<GrantedAuthority> = setOf(
            SimpleGrantedAuthority("${prefix1.value}${AuthorityMappingService.AUTHORITY_DELIMITER}1"),
            SimpleGrantedAuthority("${prefix1.value}${AuthorityMappingService.AUTHORITY_DELIMITER}2"),
            SimpleGrantedAuthority("${prefix2.value}${AuthorityMappingService.AUTHORITY_DELIMITER}3"),
            SimpleGrantedAuthority("4")
        )
        val longs = authorityMappingService.extractLongValues(prefix1, authorities)

        assertThat(longs).isEqualTo(setOf(1L, 2L))
    }

    @Test
    fun `given prefix and authorities should return string values`() {
        val prefix1 = AuthorityPrefix.CLIENT_ID
        val prefix2 = AuthorityPrefix.ROLE

        val authorities: Set<GrantedAuthority> = setOf(
            SimpleGrantedAuthority("${prefix1.value}${AuthorityMappingService.AUTHORITY_DELIMITER}a"),
            SimpleGrantedAuthority("${prefix1.value}${AuthorityMappingService.AUTHORITY_DELIMITER}b"),
            SimpleGrantedAuthority("${prefix2.value}${AuthorityMappingService.AUTHORITY_DELIMITER}c"),
            SimpleGrantedAuthority("d")
        )
        val strings = authorityMappingService.extractStringValues(AuthorityPrefix.CLIENT_ID, authorities)

        assertThat(strings).isEqualTo(setOf("a", "b"))
    }
}
