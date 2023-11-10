package ru.citeck.ecos.records3.test.op.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory

class LocalIdAndAppNameTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val dto = TestDto1(RecordRef.create("aa", "bb", "cc"), "123")

        assertThat(records.getAtt(dto, "_localId").asText()).isEqualTo("cc")
        assertThat(records.getAtt(dto, "?localId").asText()).isEqualTo("cc")
        assertThat(records.getAtt(dto, "_appName").asText()).isEqualTo("aa")
        assertThat(records.getAtt(dto, "?appName").asText()).isEqualTo("aa")

        val dto2 = TestDto2("dd", "123")

        assertThat(records.getAtt(dto2, "_localId").asText()).isEqualTo("dd")
        assertThat(records.getAtt(dto2, "?localId").asText()).isEqualTo("dd")
        assertThat(records.getAtt(dto2, "_appName").asText()).isEqualTo("")
        assertThat(records.getAtt(dto2, "?appName").asText()).isEqualTo("")
    }

    data class TestDto1(val id: RecordRef, val field: String)
    data class TestDto2(val id: String, val field: String)
}
