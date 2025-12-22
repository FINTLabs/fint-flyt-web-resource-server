package no.novari.flyt.webresourceserver.security.user.permission

import no.novari.cache.FintCache
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.util.UUID

class UserPermissionCachingListenerFactory {
    fun create(
        containerFactoryService: ParameterizedListenerContainerFactoryService,
        userPermissionCache: FintCache<UUID, UserPermission>,
        errorHandlerFactory: ErrorHandlerFactory,
    ): ConcurrentMessageListenerContainer<String, UserPermission> =
        containerFactoryService
            .createBatchListenerContainerFactory(
                UserPermission::class.java,
                { consumerRecords ->
                    consumerRecords.forEach { consumerRecord ->
                        log.debug(
                            "Consuming user permission: {} {}",
                            consumerRecord.key(),
                            consumerRecord.value().sourceApplicationIds,
                        )
                        userPermissionCache.put(
                            UUID.fromString(consumerRecord.key()),
                            consumerRecord.value(),
                        )
                    }
                },
                ListenerConfiguration
                    .stepBuilder()
                    .groupIdApplicationDefault()
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .seekToBeginningOnAssignment()
                    .build(),
                errorHandlerFactory.createErrorHandler(
                    ErrorHandlerConfiguration
                        .stepBuilder<UserPermission>()
                        .noRetries()
                        .skipFailedRecords()
                        .build(),
                ),
            ).createContainer(
                EntityTopicNameParameters
                    .builder()
                    .topicNamePrefixParameters(
                        TopicNamePrefixParameters
                            .stepBuilder()
                            .orgIdApplicationDefault()
                            .domainContextApplicationDefault()
                            .build(),
                    ).resourceName("userpermission")
                    .build(),
            )

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
