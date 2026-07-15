package no.novari.flyt.webresourceserver.security.user.authorization

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import java.time.Duration
import java.util.UUID

class CachingUserAuthorizationClient(
    private val delegate: UserAuthorizationClient,
    private val properties: UserAuthorizationClientProperties.Cache,
    private val ticker: Ticker = Ticker.systemTicker(),
) : UserAuthorizationClient {
    private val cache =
        Caffeine
            .newBuilder()
            .maximumSize(properties.maxSize)
            .expireAfterWrite(properties.ttl.plus(properties.staleIfError))
            .ticker(ticker)
            .build<CacheKey, CacheEntry>()

    override fun getAuthorizedSourceApplicationIds(
        objectIdentifier: UUID,
        sourceApplicationIds: Set<Long>,
    ): Set<Long> {
        if (sourceApplicationIds.isEmpty()) return emptySet()

        val now = ticker.read()
        val cachedEntries =
            sourceApplicationIds.associateWith { sourceApplicationId ->
                cache.getIfPresent(CacheKey(objectIdentifier, sourceApplicationId))
            }
        val sourceApplicationIdsToRefresh =
            cachedEntries
                .filterValues { entry -> entry == null || !entry.isFresh(now, properties.ttl) }
                .keys

        if (sourceApplicationIdsToRefresh.isEmpty()) {
            return authorizedSourceApplicationIds(sourceApplicationIds, cachedEntries)
        }

        return try {
            val authorizedSourceApplicationIds =
                delegate.getAuthorizedSourceApplicationIds(objectIdentifier, sourceApplicationIdsToRefresh)
            val refreshedAt = ticker.read()

            sourceApplicationIdsToRefresh.forEach { sourceApplicationId ->
                cache.put(
                    CacheKey(objectIdentifier, sourceApplicationId),
                    CacheEntry(
                        authorized = sourceApplicationId in authorizedSourceApplicationIds,
                        cachedAtNanos = refreshedAt,
                    ),
                )
            }

            sourceApplicationIds.filterTo(mutableSetOf()) { sourceApplicationId ->
                when {
                    sourceApplicationId in sourceApplicationIdsToRefresh -> {
                        sourceApplicationId in authorizedSourceApplicationIds
                    }

                    else -> {
                        cachedEntries.getValue(sourceApplicationId)?.authorized == true
                    }
                }
            }
        } catch (exception: UserAuthorizationClientException) {
            if (
                sourceApplicationIdsToRefresh.all { sourceApplicationId ->
                    cachedEntries[sourceApplicationId]?.isWithinStaleWindow(
                        now,
                        properties.ttl,
                        properties.staleIfError,
                    ) == true
                }
            ) {
                authorizedSourceApplicationIds(sourceApplicationIds, cachedEntries)
            } else {
                throw exception
            }
        }
    }

    private fun authorizedSourceApplicationIds(
        sourceApplicationIds: Set<Long>,
        cachedEntries: Map<Long, CacheEntry?>,
    ): Set<Long> =
        sourceApplicationIds.filterTo(mutableSetOf()) { sourceApplicationId ->
            cachedEntries[sourceApplicationId]?.authorized == true
        }

    private data class CacheEntry(
        val authorized: Boolean,
        val cachedAtNanos: Long,
    ) {
        fun isFresh(
            now: Long,
            ttl: Duration,
        ): Boolean = ageNanos(now) < ttl.toNanos()

        fun isWithinStaleWindow(
            now: Long,
            ttl: Duration,
            staleIfError: Duration,
        ): Boolean = ageNanos(now) < ttl.plus(staleIfError).toNanos()

        private fun ageNanos(now: Long): Long = now - cachedAtNanos
    }

    private data class CacheKey(
        val objectIdentifier: UUID,
        val sourceApplicationId: Long,
    )
}
