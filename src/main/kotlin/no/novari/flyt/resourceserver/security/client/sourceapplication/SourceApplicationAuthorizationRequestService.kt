package no.novari.flyt.resourceserver.security.client.sourceapplication

import java.time.Duration
import java.util.Optional
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.requestreply.RequestProducerRecord
import no.novari.kafka.requestreply.RequestTemplate
import no.novari.kafka.requestreply.RequestTemplateFactory
import no.novari.kafka.requestreply.topic.ReplyTopicService
import no.novari.kafka.requestreply.topic.configuration.ReplyTopicConfiguration
import no.novari.kafka.requestreply.topic.name.ReplyTopicNameParameters
import no.novari.kafka.requestreply.topic.name.RequestTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SourceApplicationAuthorizationRequestService(
    @Value("\${novari.kafka.application-id}") applicationId: String,
    requestTemplateFactory: RequestTemplateFactory,
    replyTopicService: ReplyTopicService
) {

    private val requestTopicNameParameters: RequestTopicNameParameters =
        RequestTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters.stepBuilder()
                    .orgIdApplicationDefault()
                    .domainContextApplicationDefault()
                    .build()
            )
            .resourceName("authorization")
            .parameterName("client-id")
            .build()

    private val requestTemplate: RequestTemplate<String, SourceApplicationAuthorization>

    init {
        val replyTopicNameParameters = ReplyTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters.stepBuilder()
                    .orgIdApplicationDefault()
                    .domainContextApplicationDefault()
                    .build()
            )
            .applicationId(applicationId)
            .resourceName("authorization")
            .build()

        replyTopicService.createOrModifyTopic(
            replyTopicNameParameters,
            ReplyTopicConfiguration.builder()
                .retentionTime(Duration.ofMinutes(2))
                .build()
        )

        requestTemplate = requestTemplateFactory.createTemplate(
            replyTopicNameParameters,
            String::class.java,
            SourceApplicationAuthorization::class.java,
            Duration.ofSeconds(5),
            ListenerConfiguration.stepBuilder()
                .groupIdApplicationDefault()
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .continueFromPreviousOffsetOnAssignment()
                .build()
        )
    }

    fun getClientAuthorization(clientId: String): Optional<SourceApplicationAuthorization> = Optional.ofNullable(
        requestTemplate.requestAndReceive(
            RequestProducerRecord.builder<String>()
                .topicNameParameters(requestTopicNameParameters)
                .value(clientId)
                .build()
        ).value()
    )
}
