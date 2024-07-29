package ru.citeck.ecos.records2.predicate.json.std

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.*

class PredicateJsonSerializer : StdSerializer<Predicate>(
    Predicate::class.java
) {
    companion object {
        private const val FIELD_TYPE = "t"
        private const val FIELD_ATT = "att"
        private const val FIELD_VAL = "val"
    }

    override fun serialize(
        predicate: Predicate,
        generator: JsonGenerator,
        serializerProvider: SerializerProvider
    ) {
        serializeImpl(PredicateUtils.optimize(predicate), generator)
    }

    private fun writeType(generator: JsonGenerator, type: String) {
        generator.writeStringField(FIELD_TYPE, type)
    }

    private fun serializeImpl(predicate: Predicate, generator: JsonGenerator) {
        generator.writeStartObject()
        when (predicate) {
            is ValuePredicate -> {
                writeType(generator, predicate.getType().asString())
                generator.writeStringField(FIELD_ATT, predicate.getAttribute())
                generator.writeFieldName(FIELD_VAL)
                generator.writeTree(predicate.getValue().asJson())
            }
            is EmptyPredicate -> {
                writeType(generator, EmptyPredicate.TYPE)
                generator.writeStringField(FIELD_ATT, predicate.getAttribute())
            }
            is ComposedPredicate -> {
                val type = when (predicate) {
                    is AndPredicate -> AndPredicate.TYPE
                    is OrPredicate -> OrPredicate.TYPE
                    else -> error("unknown composed type: ${predicate::class}")
                }
                writeType(generator, type)
                generator.writeArrayFieldStart(FIELD_VAL)
                val predicates = predicate.getPredicates()
                for (innerPred in predicates) {
                    serializeImpl(innerPred, generator)
                }
                generator.writeEndArray()
            }
            is NotPredicate -> {
                writeType(generator, NotPredicate.TYPE)
                generator.writeFieldName(FIELD_VAL)
                serializeImpl(predicate.getPredicate(), generator)
            }
            is VoidPredicate -> {} // empty object
            else -> error("Unknown predicate type: " + predicate::class)
        }
        generator.writeEndObject()
    }
}
