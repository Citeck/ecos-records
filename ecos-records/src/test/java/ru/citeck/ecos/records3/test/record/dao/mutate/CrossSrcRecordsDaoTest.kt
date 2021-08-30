package ru.citeck.ecos.records3.test.record.dao.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateCrossSrcDao

class CrossSrcRecordsDaoTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(CrossSrcDao())

        val res = records.mutate("test@", emptyMap<String, Any>())

        assertThat(res).isEqualTo(RecordRef.create("other", "test"))
    }

    class CrossSrcDao : RecordsMutateCrossSrcDao {
        override fun getId() = "test"
        override fun mutate(records: List<LocalRecordAtts>): List<RecordRef> {
            return listOf(RecordRef.create("other", "test"))
        }
    }
}
