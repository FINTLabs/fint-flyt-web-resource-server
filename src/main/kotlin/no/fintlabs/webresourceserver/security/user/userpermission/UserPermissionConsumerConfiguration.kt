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
    private val log = LoggerFactory.getLogger(UserPermissionConsumerConfiguration::class.java)

    @Bean
    @ConditionalOnProperty(
        value = ["fint.flyt.webresourceserver.user-permissions-consumer.enabled"],
        havingValue = "true",
    )
    fun createCacheConsumer(): ConcurrentMessageListenerContainer<String, UserPermission> {
        return entityConsumerFactoryService
            .createBatchConsumerFactory(
                UserPermission::class.java,
            ) { consumerRecords ->
                consumerRecords.forEach { consumerRecord ->
                    log.info(
                        "Consuming userpermission: {} {}",
                        consumerRecord.key(),
                        consumerRecord.value().sourceApplicationIds,
                    )

                    userPermissionCache.put(
                        consumerRecord.key(),
                        consumerRecord.value(),
                    )
                }
            }.createContainer(EntityTopicNameParameters.builder().resource("userpermission").build())
    }
}
