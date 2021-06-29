package ru.citeck.ecos.records3.test.record.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName

class RecordRefAttValueTest {

    @Test
    fun test() {

        val refValueDto = RefValueDto("str-value")

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("ref-value", refValueDto)
                .build()
        )

        val dtoWithRef = DtoWithRef(RecordRef.create("test", "ref-value"))

        assertThat(records.getAtt(dtoWithRef, "ref.str").asText()).isEqualTo(refValueDto.str)
        assertThat(records.getAtt(dtoWithRef, "ref.cm:attWithDots").asText()).isEqualTo(refValueDto.getAttWithDots())
    }

    class DtoWithRef(
        val ref: RecordRef
    )

    class RefValueDto(
        val str: String
    ) {
        @AttName("cm:attWithDots")
        fun getAttWithDots(): String {
            return "attWithDots"
        }
    }
}
