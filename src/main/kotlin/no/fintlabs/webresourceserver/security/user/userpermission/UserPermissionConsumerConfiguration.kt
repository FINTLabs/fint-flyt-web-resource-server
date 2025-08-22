package no.fintlabs.webresourceserver.security.user.userpermission

import no.fintlabs.cache.FintCache
import no.fintlabs.kafka.consuming.ErrorHandlerConfiguration
import no.fintlabs.kafka.consuming.ListenerConfiguration
import no.fintlabs.kafka.consuming.ParameterizedListenerContainerFactoryService
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
@Import(
    ParameterizedListenerContainerFactoryService::class,
    UserPermissionCacheConfiguration::class,
)
class UserPermissionConsumerConfiguration(
    private val factoryService: ParameterizedListenerContainerFactoryService,
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
        val errorHandler =
            ErrorHandlerConfiguration
                .builder(UserPermission::class.java)
                .noRetries()
                .skipFailedRecords()
                .build()

        val config =
            ListenerConfiguration
                .builder(UserPermission::class.java)
                .groupIdApplicationDefault()
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .errorHandler(errorHandler)
                .seekToBeginningOnAssignment()
                .build()

        return factoryService
            .createBatchListenerContainerFactory(
                { records: List<ConsumerRecord<String, UserPermission>> ->
                    records.forEach { record ->
                        log.info(
                            "Consuming userpermission: {} {}",
                            record.key(),
                            record.value().sourceApplicationIds,
                        )

                        userPermissionCache.put(
                            record.key(),
                            record.value(),
                        )
                    }
                },
                config,
            ).createContainer(
                EntityTopicNameParameters
                    .builder()
                    .resourceName(RESOURCE_USER_PERMISSION)
                    .build(),
            )
    }
}
