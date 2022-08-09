package ru.citeck.ecos.records3.record.atts.schema.write

import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

class AttSchemaLegacyWriter : AttSchemaWriterV2() {

    companion object {
        private val SCALAR_MAPPING = mapOf(
            ScalarType.RAW.schema to ScalarType.STR.schema
        )
    }

    override fun write(attribute: SchemaAtt, out: StringBuilder, firstInBraces: Boolean) {
        val schemaAtt = attribute.withName(SCALAR_MAPPING.getOrDefault(attribute.name, attribute.name))
        if (schemaAtt.isScalar()) {
            val schema = schemaAtt.name.substring(1)
            if (out.isEmpty()) {
                out.append('.').append(schema)
            } else {
                if (schemaAtt.alias.isNotEmpty()) {
                    out.append(schemaAtt.alias)
                        .append(":")
                }
                out.append(".$schema")
            }
        } else if (schemaAtt.name == "_has" && schemaAtt.inner.size == 1) {
            if (schemaAtt.alias.isNotEmpty()) {
                out.append(schemaAtt.alias)
                    .append(":")
            }
            out.append(".has(n:\"")
                .append(schemaAtt.inner[0].name)
                .append("\")")
        } else if (schemaAtt.name == "_edge") {
            if (schemaAtt.alias.isNotEmpty()) {
                out.append(schemaAtt.alias)
                    .append(":")
            }
            val inner = schemaAtt.inner[0]
            out.append(".edge(n:\"")
                .append(inner.name)
                .append("\"){")
                .append(inner.inner[0].name)
                .append("}")
        } else {
            super.write(schemaAtt, out, firstInBraces)
        }
    }
}
