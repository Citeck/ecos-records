package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory

class AttInverseProcessorTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsService
        assertThat(records.getAtt(222, "_self|eq(222)|inv()")).isEqualTo(DataValue.FALSE)
        assertThat(records.getAtt(222, "_self|eq(223)|inv()")).isEqualTo(DataValue.TRUE)
    }
}
