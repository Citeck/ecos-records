package ru.citeck.ecos.records3.json

import ru.citeck.ecos.commons.json.JsonMapperConfigurer
import ru.citeck.ecos.commons.json.MappingContext
import ru.citeck.ecos.records2.predicate.json.std.PredicateJsonDeserializer
import ru.citeck.ecos.records2.predicate.json.std.PredicateJsonSerializer
import ru.citeck.ecos.records2.predicate.json.std.PredicateTypes

class RecordsJsonMapperConfigurer : JsonMapperConfigurer {

    override fun configure(context: MappingContext) {
        configurePredicates(context)
    }

    private fun configurePredicates(context: MappingContext) {

        val predicateTypes = PredicateTypes()
        val predicateJsonDeserializer = PredicateJsonDeserializer(predicateTypes)

        context.addDeserializer(predicateJsonDeserializer)
        context.addSerializer(PredicateJsonSerializer())
    }
}
