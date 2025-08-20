package no.fintlabs.webresourceserver.security.user

import no.fintlabs.cache.FintCache
import no.fintlabs.webresourceserver.security.user.userpermission.UserPermission
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserClaimFormattingService(
    private val userPermissionCache: FintCache<String, UserPermission>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(UserClaimFormattingService::class.java)
    }

    fun removeDoubleQuotesFromClaim(claim: String): String {
        return claim.replace("\\", "").replace("\"", "")
    }

    fun convertSourceApplicationIdsIntoString(objectIdentifier: String?): String {
        var sourceApplicationIdsString = ""
        if (objectIdentifier != null) {
            val userPermissionOptional = userPermissionCache.getOptional(objectIdentifier)
            if (userPermissionOptional.isPresent) {
                val sourceApplicationIds = userPermissionOptional.get().sourceApplicationIds
                sourceApplicationIdsString = sourceApplicationIds.joinToString(",") { it.toString() }
                log.debug("Fetched sourceApplicationIds from cache: $sourceApplicationIdsString")
            }
        }
        return sourceApplicationIdsString
    }
}
