package ru.citeck.ecos.records3.test.record.atts.schema.read

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao

class DtoSchemaReaderTest {

    @Test
    fun arrayTest() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao("test"))

        val elements = listOf("abc", "def", 123, 456.1)

        val ref = records.create(
            "test",
            mapOf(
                "array" to elements
            )
        )

        val atts = records.getAtts(ref, AttsDto::class.java)
        assertThat(atts.array).containsExactlyElementsOf(
            elements.map { DataValue.create(it) }
        )
        assertThat(atts.annotatedArray).containsExactlyElementsOf(
            elements.map { DataValue.create(it) }
        )
        assertThat(atts.annotatedArray2).containsExactlyElementsOf(
            elements.map { DataValue.create(it) }
        )
        assertThat(atts.single).isEqualTo(DataValue.create(elements[0]))
    }

    class AttsDto(
        val array: List<DataValue>,
        @AttName("array")
        val annotatedArray: List<DataValue>,
        @AttName("array[]")
        val annotatedArray2: List<DataValue>,
        @AttName("array")
        val single: DataValue
    )
}
