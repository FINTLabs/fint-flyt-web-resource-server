package no.novari.flyt.webresourceserver.security.client.sourceapplication

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Duration

/**
 * Looks up the source application of a client in fint-flyt-authorization-service.
 *
 * The lookup runs for every incoming request on the external API, so found clients are cached for [CACHE_TTL].
 * Returns `null` on 404 without caching it, so a newly registered client is picked up at once.
 * Other errors are thrown and not cached.
 */
class SourceApplicationAuthorizationClient(
    private val restClient: RestClient,
) {
    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .maximumSize(CACHE_MAX_SIZE)
            .build<String, SourceApplicationAuthorization>()

    fun getClientAuthorization(clientId: String): SourceApplicationAuthorization? =
        cache.getIfPresent(clientId) ?: fetch(clientId)?.also { cache.put(clientId, it) }

    private fun fetch(clientId: String): SourceApplicationAuthorization? =
        try {
            restClient
                .get()
                .uri("/clients/{clientId}", clientId)
                .retrieve()
                .body<SourceApplicationAuthorization>()
        } catch (_: HttpClientErrorException.NotFound) {
            null
        }

    private companion object {
        val CACHE_TTL: Duration = Duration.ofMinutes(1)
        const val CACHE_MAX_SIZE = 1000L
    }
}
