package ru.citeck.ecos.records3.spring.predicate.jpa

import org.springframework.data.jpa.domain.Specification
import ru.citeck.ecos.records2.predicate.model.Predicate

interface PredicateJpaService {

    fun <T : Any> createSpec(predicate: Predicate, entityType: Class<T>): Specification<T>
}
