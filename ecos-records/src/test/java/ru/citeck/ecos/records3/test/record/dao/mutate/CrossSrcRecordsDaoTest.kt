package ru.citeck.ecos.records3.test.record.dao.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateCrossSrcDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

class CrossSrcRecordsDaoTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsService
        records.register(CrossSrcDao())

        val res = records.mutate("test@", emptyMap<String, Any>())

        assertThat(res).isEqualTo(EntityRef.create("other", "test"))
    }

    class CrossSrcDao : RecordsMutateCrossSrcDao {
        override fun getId() = "test"
        override fun mutate(records: List<LocalRecordAtts>): List<EntityRef> {
            return listOf(EntityRef.create("other", "test"))
        }
    }
}
