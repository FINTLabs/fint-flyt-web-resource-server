package no.fintlabs.webresourceserver

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Configuration

@EnableAutoConfiguration
@Configuration
class ResourceServerConfiguration {
    @Value("\${fint.cache.defaultCacheEntryTimeToLiveMillis:9223372036854775807}")
    private var defaultCacheEntryTimeToLiveMillis: Long = 9223372036854775807

    @Value("\${fint.cache.defaultCacheHeapSize:1000000}")
    private var defaultCacheHeapSize: Long = 1000000

    @PostConstruct
    fun configureDefault() {
        System.setProperty("fint.cache.defaultCacheEntryTimeToLiveMillis", defaultCacheEntryTimeToLiveMillis.toString())
        System.setProperty("fint.cache.defaultCacheHeapSize", defaultCacheHeapSize.toString())
    }
}
