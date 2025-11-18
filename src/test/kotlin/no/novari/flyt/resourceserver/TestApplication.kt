package no.novari.flyt.resourceserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@SpringBootApplication
@ComponentScan(
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [ConcurrentMessageListenerContainer::class],
        ),
    ],
)
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}
