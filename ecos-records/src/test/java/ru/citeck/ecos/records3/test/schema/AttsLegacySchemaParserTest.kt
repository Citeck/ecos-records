package ru.citeck.ecos.records3.test.schema

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.request.rest.AttsLegacySchemaParser
import ru.citeck.ecos.records3.RecordsServiceFactory

class AttsLegacySchemaParserTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val parser = AttsLegacySchemaParser(services)

        val schema = "a:att(n:\"username\"){disp}," +
            "b:att(n:\"permissions\"){has(n:\"Write\")}," +
            "c:att(n:\"name\"){disp}," +
            "d:has(n:\"cm:content\")," +
            "e:att(n:\"module_id\"){disp}"
        val result = HashMap(parser.parse(schema) ?: emptyMap())

        val expected = hashMapOf(
            "a" to "username",
            "b" to "permissions._has.Write?bool",
            "c" to "name",
            "d" to "_has.cm:content?bool",
            "e" to "module_id"
        )
        assertThat(result).isEqualTo(expected)
    }
}
