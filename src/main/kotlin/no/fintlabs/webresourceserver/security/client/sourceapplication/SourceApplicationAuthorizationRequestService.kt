package no.fintlabs.webresourceserver.security.client.sourceapplication

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class SourceApplicationAuthorizationRequestService(
    @Value("\${fint.kafka.application-id}") applicationId: String,
//    requestProducerFactory: RequestProducerFactory,
//    replyTopicService: ReplyTopicService,
) {
//    private val requestTopicNameParameters: RequestTopicNameParameters =
//        RequestTopicNameParameters
//            .builder()
//            .resource("authorization")
//            .parameterName("client-id")
//            .build()
//
//    private lateinit var requestProducer: RequestProducer<String, SourceApplicationAuthorization>
//
//    init {
//        val replyTopicNameParameters =
//            ReplyTopicNameParameters
//                .builder()
//                .applicationId(applicationId)
//                .resource("authorization")
//                .build()
//        replyTopicService.ensureTopic(
//            replyTopicNameParameters,
//            0,
//            TopicCleanupPolicyParameters.builder().build(),
//        )
//        requestProducer =
//            requestProducerFactory.createProducer(
//                replyTopicNameParameters,
//                String::class.java,
//                SourceApplicationAuthorization::class.java,
//            )
//    }

    fun getClientAuthorization(clientId: String): Optional<SourceApplicationAuthorization> {
//        return requestProducer
//            .requestAndReceive(
//                RequestProducerRecord
//                    .builder<String>()
//                    .topicNameParameters(requestTopicNameParameters)
//                    .value(clientId)
//                    .build(),
//            ).map(ConsumerRecord<String, SourceApplicationAuthorization>::value)
        return TODO("Provide the return value")
    }
}
