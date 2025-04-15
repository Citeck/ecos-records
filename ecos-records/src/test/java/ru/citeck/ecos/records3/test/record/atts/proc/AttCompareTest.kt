package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory

class AttCompareTest {

    @Test
    fun test() {
        val records = RecordsServiceFactory().recordsService
        assertThat(records.getAtt(0, "_self|eq(0)")).isEqualTo(DataValue.TRUE)
        assertThat(records.getAtt(0, "_self|eq(1)")).isEqualTo(DataValue.FALSE)
        assertThat(records.getAtt("VAL", "?localId|eq('VAL')")).isEqualTo(DataValue.TRUE)
        assertThat(records.getAtt("VAL2", "?localId|eq('VAL')")).isEqualTo(DataValue.FALSE)
        assertThat(records.getAtt(10, "_self|gt(10)")).isEqualTo(DataValue.FALSE)
        assertThat(records.getAtt(10, "_self|gt(9)")).isEqualTo(DataValue.TRUE)
        assertThat(records.getAtt(10, "_self|ge(10)")).isEqualTo(DataValue.TRUE)
        assertThat(records.getAtt(10, "_self|le(10)")).isEqualTo(DataValue.TRUE)
        assertThat(records.getAtt(10, "_self|lt(10)")).isEqualTo(DataValue.FALSE)
        assertThat(records.getAtt("aaaabbb", "?localId|contains('ab')")).isEqualTo(DataValue.TRUE)
        assertThat(records.getAtt("aaaabbb", "?localId|contains('abc')")).isEqualTo(DataValue.FALSE)
    }
}
