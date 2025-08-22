package no.fintlabs.webresourceserver.security.client.sourceapplication

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service

@Service
class SourceApplicationAuthorizationService {
    companion object {
        const val SOURCE_APPLICATION_ID_PREFIX = "SOURCE_APPLICATION_ID_"
    }

    fun getAuthority(sourceApplicationAuthorization: SourceApplicationAuthorization): GrantedAuthority {
        return SimpleGrantedAuthority(
            "$SOURCE_APPLICATION_ID_PREFIX${sourceApplicationAuthorization.sourceApplicationId}",
        )
    }

    fun getSourceApplicationId(authentication: Authentication): Long {
        val id =
            authentication.authorities
                .map { it.authority }
                .firstOrNull { it.startsWith(SOURCE_APPLICATION_ID_PREFIX) }
                ?.removePrefix(SOURCE_APPLICATION_ID_PREFIX)
                ?.toLong()

        return id ?: throw NoSuchElementException("No authority found with prefix $SOURCE_APPLICATION_ID_PREFIX")
    }
}
