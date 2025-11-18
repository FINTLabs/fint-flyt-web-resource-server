package no.novari.flyt.resourceserver.security.client.sourceapplication

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import no.novari.flyt.resourceserver.security.client.sourceapplication.exceptions.MultipleSourceApplicationIdsException
import no.novari.flyt.resourceserver.security.client.sourceapplication.exceptions.NoSourceApplicationIdException
import org.springframework.security.core.Authentication

class SourceApplicationAuthorizationService(
    private val authorityMappingService: AuthorityMappingService,
) {
    fun getSourceApplicationId(authentication: Authentication): Long {
        val sourceApplicationIds =
            authorityMappingService.extractLongValues(
                AuthorityPrefix.SOURCE_APPLICATION_ID,
                authentication.authorities,
            )
        if (sourceApplicationIds.size > 1) {
            throw MultipleSourceApplicationIdsException(sourceApplicationIds)
        }
        return sourceApplicationIds.firstOrNull() ?: throw NoSourceApplicationIdException()
    }
}
