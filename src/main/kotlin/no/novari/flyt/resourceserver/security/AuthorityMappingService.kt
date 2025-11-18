package no.novari.flyt.resourceserver.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service

@Service
class AuthorityMappingService {

    fun toAuthority(prefix: AuthorityPrefix, value: String): String =
        "${prefix.value}$AUTHORITY_DELIMITER$value"

    fun extractLongValues(
        prefix: AuthorityPrefix,
        authorities: Collection<GrantedAuthority>
    ): Set<Long> = extractStringValues(prefix, authorities)
        .mapTo(mutableSetOf(), String::toLong)

    fun extractStringValues(
        prefix: AuthorityPrefix,
        authorities: Collection<GrantedAuthority>
    ): Set<String> = authorities
        .map(GrantedAuthority::getAuthority)
        .filter { it.startsWith(prefix.value) }
        .mapTo(
            mutableSetOf()
        ) { it.substring(prefix.value.length + AUTHORITY_DELIMITER.length) }

    companion object {
        const val AUTHORITY_DELIMITER = "_"
    }
}
