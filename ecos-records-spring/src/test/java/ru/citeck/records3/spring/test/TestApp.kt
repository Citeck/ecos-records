package ru.citeck.records3.spring.test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import ru.citeck.ecos.records3.RecordsProperties

@SpringBootApplication
open class TestApp {

    @Bean
    @ConditionalOnMissingBean(RecordsProperties::class)
    open fun recordsProps(): RecordsProperties {
        return RecordsProperties()
    }
}
