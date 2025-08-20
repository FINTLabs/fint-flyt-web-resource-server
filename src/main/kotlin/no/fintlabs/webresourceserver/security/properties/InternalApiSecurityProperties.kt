package no.fintlabs.webresourceserver.security.properties

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory

class InternalApiSecurityProperties(
    var authorizedOrgIdRolePairsJson: String = "{}",
    var adminRole: String = "",
    private var authorizedOrgIdRolePairs: Map<String, List<String>> = emptyMap(),
) : ApiSecurityProperties() {
    companion object {
        private val log = LoggerFactory.getLogger(InternalApiSecurityProperties::class.java)
        const val ORGID_PREFIX = "ORGID_"
        const val ROLE_PREFIX = "_ROLE_"
    }

    @PostConstruct
    fun parseAndSetAuthorizedOrgIdRolePairs() {
        val mapper = ObjectMapper()
        try {
            authorizedOrgIdRolePairs =
                mapper.readValue(
                    authorizedOrgIdRolePairsJson,
                    object : TypeReference<Map<String, List<String>>>() {},
                )
            log.info("Authorized orgIds: {}", authorizedOrgIdRolePairs)
        } catch (e: Exception) {
            log.error("Error parsing authorizedOrgIdRolePairsJson: {}", e.message, e)
        }
    }

    override fun getPermittedAuthorities(): Array<String> {
        return authorizedOrgIdRolePairs
            .flatMap { (orgId, roles) ->
                roles.map { role -> "$ORGID_PREFIX$orgId$ROLE_PREFIX$role" }
            }.toTypedArray()
    }
}
