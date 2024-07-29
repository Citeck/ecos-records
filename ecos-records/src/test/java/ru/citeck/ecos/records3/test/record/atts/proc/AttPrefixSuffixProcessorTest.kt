package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory

class AttPrefixSuffixProcessorTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsService

        val rec = mapOf(
            "notEmpty" to "value",
            "nullVal" to null,
            "emptyVal" to "",
        )

        assertThat(records.getAtt(rec, "notEmpty|presuf('pre-', '-suf')").asText()).isEqualTo("pre-value-suf")
        assertThat(records.getAtt(rec, "notEmpty|presuf('pre-')").asText()).isEqualTo("pre-value")
        assertThat(records.getAtt(rec, "notEmpty|presuf('','-suf')").asText()).isEqualTo("value-suf")

        assertThat(records.getAtt(rec, "nullVal|presuf('pre-', '-suf')").asText()).isEqualTo("")
        assertThat(records.getAtt(rec, "emptyVal|presuf('pre-', '-suf')").asText()).isEqualTo("")
    }
}
