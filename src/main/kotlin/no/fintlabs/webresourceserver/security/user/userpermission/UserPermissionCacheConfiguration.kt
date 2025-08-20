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
    @Bean
    fun userPermissionCache(): FintCache<String, UserPermission> {
        return fintCacheManager.createCache(
            "userpermission",
            String::class.java,
            UserPermission::class.java,
            FintCacheOptions
                .builder()
                .timeToLive(Duration.ofMillis(9223372036854775807L))
                .heapSize(1000000L)
                .build(),
        )
    }
}
