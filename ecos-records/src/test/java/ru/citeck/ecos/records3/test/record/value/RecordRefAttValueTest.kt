package ru.citeck.ecos.records3.test.record.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordRefAttValueTest {

    @Test
    fun test() {

        val refValueDto = RefValueDto("str-value")

        val records = RecordsServiceFactory().recordsService
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("ref-value", refValueDto)
                .build()
        )

        val dtoWithRef = DtoWithRef(EntityRef.create("test", "ref-value"))

        assertThat(records.getAtt(dtoWithRef, "ref.str").asText()).isEqualTo(refValueDto.str)
        assertThat(records.getAtt(dtoWithRef, "ref.cm:attWithDots").asText()).isEqualTo(refValueDto.getAttWithDots())
    }

    class DtoWithRef(
        val ref: EntityRef
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
