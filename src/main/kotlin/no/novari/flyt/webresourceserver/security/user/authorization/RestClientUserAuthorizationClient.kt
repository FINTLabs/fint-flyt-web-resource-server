package no.novari.flyt.webresourceserver.security.user.authorization

import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.UUID

class RestClientUserAuthorizationClient(
    private val restClient: RestClient,
) : UserAuthorizationClient {
    override fun getAuthorizedSourceApplicationIds(
        objectIdentifier: UUID,
        sourceApplicationIds: Set<Long>,
    ): Set<Long> {
        if (sourceApplicationIds.isEmpty()) return emptySet()

        return try {
            val response =
                restClient
                    .post()
                    .uri("/actions/authorize-source-applications")
                    .body(SourceApplicationAuthorizationRequest(objectIdentifier, sourceApplicationIds))
                    .retrieve()
                    .body<SourceApplicationAuthorizationResponse>()
                    ?: throw IllegalStateException("Authorization service returned an empty response")
            response.authorizedSourceApplicationIds.toSet()
        } catch (exception: Exception) {
            throw UserAuthorizationClientException("Authorization service request failed", exception)
        }
    }
}

data class SourceApplicationAuthorizationRequest(
    val objectIdentifier: UUID,
    val sourceApplicationIds: Set<Long>,
)

data class SourceApplicationAuthorizationResponse(
    val authorizedSourceApplicationIds: Set<Long>,
)
