package ru.citeck.ecos.records3.test.record.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory

class DtoAttsTest {

    @Test
    fun testWithSingleAtt() {

        val records = RecordsServiceFactory().recordsService
        records.register(
            RecordsDaoBuilder
                .create("test")
                .addRecord("test", SingleAttDto(true))
                .addRecord("test2", ComplexAttsDto(SingleAttDto(true)))
                .build()
        )

        val dto = records.getAtts("test@test", SingleAttDto::class.java)
        assertThat(dto.value).isTrue

        val dto2 = records.getAtts("test@test2", ComplexAttsDto::class.java)
        assertThat(dto2.singleAtt.value).isTrue
    }

    data class SingleAttDto(
        val value: Boolean
    )

    data class ComplexAttsDto(
        val singleAtt: SingleAttDto
    )
}
