package no.novari.flyt.webresourceserver.security.properties

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import no.novari.flyt.webresourceserver.security.user.UserRole
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InternalApiSecurityProperties {
    var enabled: Boolean = false
    var authorizedOrgIdRolePairsJson: String? = null
    var userRoleFilterPerOrgId: Map<String, Set<UserRole>> = emptyMap()
        private set

    @PostConstruct
    fun parseAndSetAuthorizedOrgIdRolePairs() {
        val json = authorizedOrgIdRolePairsJson ?: return
        try {
            userRoleFilterPerOrgId =
                ObjectMapper().readValue(
                    json,
                    object : TypeReference<Map<String, Set<UserRole>>>() {},
                )
            log.info("Parsed authorizedOrgIdRolePairs: {}", userRoleFilterPerOrgId)
        } catch (e: Exception) {
            log.error("Error parsing authorizedOrgIdRolePairsJson: {}", e.message, e)
            userRoleFilterPerOrgId = emptyMap()
        }
    }

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
