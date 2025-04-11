package ru.citeck.ecos.records3.test.record.atts.schema.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

class AttContextTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsService

        val dao0 = RecordsDaoToTest("dao-0", "dao-1", listOf("att0", "att1"))
        val dao1 = RecordsDaoToTest("dao-1", "dao-2", emptyList())
        val dao2 = RecordsDaoToTest("dao-2", "", emptyList())

        listOf(dao0, dao1, dao2).forEach { records.register(it) }
        fun resetState() {
            listOf(dao0, dao1, dao2).forEach { it.ctxAtts.clear() }
        }

        val dao0Ref = EntityRef.create("dao-0", "test")
        records.getAtt(dao0Ref, "testAtt")

        assertThat(dao0.ctxAtts).hasSize(1)
        assertThat(dao0.ctxAtts[0].name).isEqualTo("ROOT")
        assertThat(dao0.ctxAtts[0].inner).hasSize(1)
        assertThat(dao0.ctxAtts[0].inner[0].name).isEqualTo("testAtt")

        assertThat(dao1.ctxAtts).hasSize(1)
        assertThat(dao1.ctxAtts[0].name).isEqualTo("ROOT")
        assertThat(dao1.ctxAtts[0].inner).hasSize(2)
        assertThat(dao1.ctxAtts[0].inner[0].name).isEqualTo("att0")
        assertThat(dao1.ctxAtts[0].inner[1].name).isEqualTo("att1")

        assertThat(dao2.ctxAtts).isEmpty()

        resetState()

        records.query(
            RecordsQuery.create()
                .withSourceId("dao-0")
                .build()
        )

        assertThat(dao0.ctxAtts).hasSize(1)
        assertThat(dao0.ctxAtts[0].name).isEqualTo("ROOT")
        assertThat(dao0.ctxAtts[0].inner[0].name).isEqualTo("?id")

        assertThat(dao1.ctxAtts).hasSize(1)
        assertThat(dao1.ctxAtts[0].name).isEqualTo("ROOT")
        assertThat(dao1.ctxAtts[0].inner).hasSize(2)
        assertThat(dao1.ctxAtts[0].inner[0].name).isEqualTo("att0")
        assertThat(dao1.ctxAtts[0].inner[1].name).isEqualTo("att1")

        assertThat(dao2.ctxAtts).hasSize(1)
        assertThat(dao2.ctxAtts[0].name).isEqualTo("ROOT")
        assertThat(dao2.ctxAtts[0].inner).hasSize(1)
        assertThat(dao2.ctxAtts[0].inner[0].name).isEqualTo("?id")

        resetState()

        records.query(
            RecordsQuery.create()
                .withSourceId("dao-0")
                .build(),
            listOf("testAtt")
        )

        assertThat(dao0.ctxAtts).hasSize(1)
        assertThat(dao0.ctxAtts[0].name).isEqualTo("ROOT")
        assertThat(dao0.ctxAtts[0].inner).hasSize(1)
        assertThat(dao0.ctxAtts[0].inner[0].name).isEqualTo("testAtt")

        assertThat(dao1.ctxAtts).hasSize(1)
        assertThat(dao1.ctxAtts[0].name).isEqualTo("ROOT")
        assertThat(dao1.ctxAtts[0].inner).hasSize(2)
        assertThat(dao1.ctxAtts[0].inner[0].name).isEqualTo("att0")
        assertThat(dao1.ctxAtts[0].inner[1].name).isEqualTo("att1")

        assertThat(dao2.ctxAtts).hasSize(1)
        assertThat(dao2.ctxAtts[0].name).isEqualTo("ROOT")
        assertThat(dao2.ctxAtts[0].inner).hasSize(1)
        assertThat(dao2.ctxAtts[0].inner[0].name).isEqualTo("?id")
    }

    class RecordsDaoToTest(
        private val id: String,
        val targetSrcId: String,
        val targetAtts: List<String>
    ) : AbstractRecordsDao(), RecordAttsDao, RecordsQueryDao {

        val ctxAtts = mutableListOf<SchemaAtt>()

        override fun getRecordAtts(recordId: String): Any? {
            ctxAtts.add(AttContext.getCurrentSchemaAtt())
            if (targetSrcId.isNotBlank() && targetAtts.isNotEmpty()) {
                recordsService.getAtts(EntityRef.create(targetSrcId, recordId), targetAtts)
            }
            return null
        }

        override fun queryRecords(recsQuery: RecordsQuery): Any? {
            ctxAtts.add(AttContext.getCurrentSchemaAtt())
            if (targetSrcId.isNotBlank()) {
                if (targetAtts.isNotEmpty()) {
                    recordsService.query(
                        RecordsQuery.create()
                            .withSourceId(targetSrcId)
                            .build(),
                        targetAtts
                    )
                } else {
                    recordsService.query(
                        RecordsQuery.create()
                            .withSourceId(targetSrcId)
                            .build()
                    )
                }
            }
            return null
        }

        override fun getId(): String {
            return id
        }
    }
}
