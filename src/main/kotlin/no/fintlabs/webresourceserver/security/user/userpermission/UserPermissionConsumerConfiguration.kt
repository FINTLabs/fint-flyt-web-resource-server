package no.fintlabs.webresourceserver.security.user.userpermission

import no.fintlabs.cache.FintCache
import no.fintlabs.kafka.entity.EntityConsumerFactoryService
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
@Import(
    EntityConsumerFactoryService::class,
    UserPermissionCacheConfiguration::class,
)
class UserPermissionConsumerConfiguration(
    private val entityConsumerFactoryService: EntityConsumerFactoryService,
    private val userPermissionCache: FintCache<String, UserPermission>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(UserPermissionConsumerConfiguration::class.java)
        private const val PROP_ENABLED = "fint.flyt.webresourceserver.user-permissions-consumer.enabled"
        private const val RESOURCE_USER_PERMISSION = "userpermission"
    }

    @Bean
    @ConditionalOnProperty(
        value = [PROP_ENABLED],
        havingValue = "true",
    )
    fun createCacheConsumer(): ConcurrentMessageListenerContainer<String, UserPermission> {
        return entityConsumerFactoryService
            .createBatchConsumerFactory(
                UserPermission::class.java,
            ) { consumerRecords ->
                for (record in consumerRecords) {
                    with(record) {
                        log.info("Consuming userpermission: {} {}", key(), value().sourceApplicationIds)
                        userPermissionCache.put(key(), value())
                    }
                }
            }.createContainer(EntityTopicNameParameters.builder().resource(RESOURCE_USER_PERMISSION).build())
    }
}
