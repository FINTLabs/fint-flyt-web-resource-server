package no.novari.flyt.webresourceserver.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service

@Service
public class AuthorityMappingService {
    public fun toAuthority(
        prefix: AuthorityPrefix,
        value: String,
    ): String = "${prefix.value}$AUTHORITY_DELIMITER$value"

    public fun extractLongValues(
        prefix: AuthorityPrefix,
        authorities: Collection<GrantedAuthority>,
    ): Set<Long> =
        extractStringValues(prefix, authorities)
            .mapTo(mutableSetOf(), String::toLong)

    public fun extractStringValues(
        prefix: AuthorityPrefix,
        authorities: Collection<GrantedAuthority>,
    ): Set<String> =
        authorities
            .map(GrantedAuthority::getAuthority)
            .filter { it.startsWith(prefix.value) }
            .mapTo(
                mutableSetOf(),
            ) { it.substring(prefix.value.length + AUTHORITY_DELIMITER.length) }

    public companion object {
        public const val AUTHORITY_DELIMITER: String = "_"
    }
}
