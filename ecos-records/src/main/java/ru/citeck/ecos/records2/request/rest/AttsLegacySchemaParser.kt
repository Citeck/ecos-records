package ru.citeck.ecos.records2.request.rest

import ru.citeck.ecos.records3.record.atts.schema.utils.AttStrUtils
import ru.citeck.ecos.records3.RecordsServiceFactory

class AttsLegacySchemaParser(services: RecordsServiceFactory) {

    private val reader = services.attSchemaReader
    private val writer = services.attSchemaWriter

    fun parse(schema: String?): Map<String, String>? {
        if (schema.isNullOrBlank()) {
            return null
        }
        val atts = AttStrUtils.split(schema, ",")
        val attsMapToRead = mutableMapOf<String, String>()
        for (att in atts) {
            val aliasAndAtt = AttStrUtils.split(att, ":")
            attsMapToRead[aliasAndAtt[0]] = "." + aliasAndAtt[1]
        }
        val parsedAtts = reader.read(attsMapToRead)
        val result = mutableMapOf<String, String>()
        parsedAtts.forEach { schemaAtt ->
            result[schemaAtt.getAliasForValue()] = writer.write(schemaAtt.withAlias(""))
        }
        return result
    }
}
