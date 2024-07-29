package ru.citeck.ecos.records3.test.record.atts.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.RecordAttValueCtx

class AttValueCtxTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsService
        val attValueCtx = RecordAttValueCtx(CustomAttValue(), records)

        assertThat(records.getAtt(attValueCtx, "field").asText()).isEqualTo("value")
        assertThat(records.getAtt(attValueCtx, "?str").asText()).isEqualTo("asText")
    }

    class CustomAttValue : AttValue {

        override fun asText(): String {
            return "asText"
        }

        override fun getAtt(name: String): Any? {
            return when (name) {
                "field" -> "value"
                else -> null
            }
        }
    }
}
