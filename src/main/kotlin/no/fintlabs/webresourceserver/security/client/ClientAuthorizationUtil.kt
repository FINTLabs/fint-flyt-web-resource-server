package no.fintlabs.webresourceserver.security.client

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class ClientAuthorizationUtil {

    companion object {
        const val CLIENT_ID_PREFIX = "CLIENT_ID_"

        @JvmStatic
        fun getAuthority(sub: String): GrantedAuthority {
            return SimpleGrantedAuthority("$CLIENT_ID_PREFIX$sub")
        }
    }

}