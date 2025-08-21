package no.fintlabs.webresourceserver.security.user.userpermission

import no.fintlabs.cache.FintCache
import no.fintlabs.cache.FintCacheConfiguration
import no.fintlabs.cache.FintCacheManager
import no.fintlabs.cache.FintCacheOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Duration

@Configuration
@Import(FintCacheConfiguration::class)
class UserPermissionCacheConfiguration(
    private val fintCacheManager: FintCacheManager,
) {
    companion object {
        private const val CACHE_NAME = "userpermission"
        private const val HEAP_SIZE = 1_000_000L
        private val TTL: java.time.Duration = java.time.Duration.ofMillis(Long.MAX_VALUE)
    }

    @Bean
    fun userPermissionCache(): FintCache<String, UserPermission> {
        return fintCacheManager.createCache(
            CACHE_NAME,
            String::class.java,
            UserPermission::class.java,
            FintCacheOptions
                .builder()
                .timeToLive(TTL)
                .heapSize(HEAP_SIZE)
                .build(),
        )
    }
}
