package no.fintlabs.webresourceserver.security.client.sourceapplication

import no.fintlabs.kafka.consuming.ErrorHandlerConfiguration
import no.fintlabs.kafka.consuming.ListenerConfiguration
import no.fintlabs.kafka.requestreply.RequestProducerRecord
import no.fintlabs.kafka.requestreply.RequestTemplate
import no.fintlabs.kafka.requestreply.RequestTemplateFactory
import no.fintlabs.kafka.requestreply.topic.ReplyTopicService
import no.fintlabs.kafka.requestreply.topic.configuration.ReplyTopicConfiguration
import no.fintlabs.kafka.requestreply.topic.name.ReplyTopicNameParameters
import no.fintlabs.kafka.requestreply.topic.name.RequestTopicNameParameters
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
class SourceApplicationAuthorizationRequestService(
    @Value("\${fint.kafka.application-id}") private val applicationId: String,
    @Value("\${fint.kafka.reply-topic.retention-hours:1}") private val replyRetentionHours: Long,
    @Value("\${fint.kafka.reply-topic.request-timeout-seconds:30}") private val requestTimeoutSeconds: Long,
    requestTemplateFactory: RequestTemplateFactory,
    replyTopicService: ReplyTopicService,
) {
    companion object {
        private const val RESOURCE_NAME = "authorization"
        private const val PARAMETER_NAME = "client-id"
    }

    private val requestTopicNameParameters: RequestTopicNameParameters =
        RequestTopicNameParameters
            .builder()
            .resourceName(RESOURCE_NAME)
            .parameterName(PARAMETER_NAME)
            .build()

    private val requestTemplate: RequestTemplate<String, SourceApplicationAuthorization>

    init {
        val replyTopicNameParameters =
            ReplyTopicNameParameters
                .builder()
                .applicationId(applicationId)
                .resourceName(RESOURCE_NAME)
                .build()

        replyTopicService.createOrModifyTopic(
            replyTopicNameParameters,
            ReplyTopicConfiguration
                .builder()
                .retentionTime(replyRetentionHours.hours.toJavaDuration())
                .build(),
        )

        requestTemplate =
            requestTemplateFactory.createTemplate(
                replyTopicNameParameters,
                String::class.java,
                requestTimeoutSeconds.seconds.toJavaDuration(),
                ListenerConfiguration
                    .builder(SourceApplicationAuthorization::class.java)
                    .groupIdApplicationDefault()
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .errorHandler(
                        ErrorHandlerConfiguration
                            .builder(SourceApplicationAuthorization::class.java)
                            .noRetries()
                            .skipFailedRecords()
                            .build(),
                    ).continueFromPreviousOffsetOnAssignment()
                    .build(),
            )
    }

    fun getClientAuthorization(clientId: String): SourceApplicationAuthorization? {
        return requestTemplate
            .requestAndReceive(
                RequestProducerRecord(
                    requestTopicNameParameters,
                    clientId,
                    clientId,
                ),
            ).value()
    }
}
