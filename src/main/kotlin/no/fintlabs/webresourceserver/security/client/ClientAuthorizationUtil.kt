package no.fintlabs.webresourceserver.security.client

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

object ClientAuthorizationUtil {
    const val CLIENT_ID_PREFIX = "CLIENT_ID_"

    @JvmStatic
    fun getAuthority(sub: String): GrantedAuthority {
        return SimpleGrantedAuthority("$CLIENT_ID_PREFIX$sub")
    }
}
