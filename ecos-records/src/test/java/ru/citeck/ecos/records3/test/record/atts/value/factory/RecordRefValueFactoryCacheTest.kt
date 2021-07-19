package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import java.util.concurrent.atomic.AtomicInteger

class RecordRefValueFactoryCacheTest {

    @Test
    fun testWithQuery() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(object : RecordsQueryDao, RecordAttsDao {
            override fun getId() = "test"
            override fun getRecordAtts(recordId: String): Any {
                return RecordRef.create("refs", recordId)
            }
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                return listOf("ref0", "ref1", "ref2").map {
                    mapOf("ref" to RecordRef.create("refs", it))
                }
            }
        })

        val getAttsCounters = HashMap<String, AtomicInteger>()
        val recordsById = HashMap<String, RefValue>()
        // DAO for refs
        records.register(object : RecordAttsDao {
            override fun getId() = "refs"
            override fun getRecordAtts(recordId: String): Any {
                getAttsCounters.computeIfAbsent(recordId) { AtomicInteger() }.incrementAndGet()
                return recordsById.computeIfAbsent(recordId) { RefValue(it) }
            }
        })

        val queryResult = records.query(
            RecordsQuery.create {
                withSourceId("test")
            },
            mapOf(
                "attKey" to "ref.ref10?disp"
            )
        ).getRecords()

        assertThat(queryResult).hasSize(3)
        assertThat(queryResult).allMatch { it.getAtt("attKey").asText() == "ref10" }

        assertThat(getAttsCounters["ref10"]!!.get()).isEqualTo(1)
        assertThat(recordsById["ref10"]!!.attCounters["?disp"]!!.get()).isEqualTo(1)
    }

    @Test
    fun mutTest() {

        val records = RecordsServiceFactory().recordsServiceV1

        records.register(object : RecordsQueryDao, RecordAttsDao {
            override fun getId() = "test"
            override fun getRecordAtts(recordId: String): Any {
                return RecordRef.create("refs", recordId)
            }
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                return listOf("ref0", "ref1", "ref2").map {
                    mapOf("ref" to RecordRef.create("refs", it))
                }
            }
        })

        val getAttsCounters = HashMap<String, AtomicInteger>()
        val recordsById = HashMap<String, RefValue>()
        // DAO for refs
        records.register(object : RecordAttsDao {
            override fun getId() = "refs"
            override fun getRecordAtts(recordId: String): Any {
                getAttsCounters.computeIfAbsent(recordId) { AtomicInteger() }.incrementAndGet()
                return recordsById.computeIfAbsent(recordId) { RefValue(it) }
            }
        })
    }

    class RefValue(val id: String) : AttValue {

        val attCounters = HashMap<String, AtomicInteger>()

        override fun getDisplayName(): String {
            attCounters.computeIfAbsent("?disp") { AtomicInteger() }.incrementAndGet()
            return id
        }

        override fun getAtt(name: String): Any {
            attCounters.computeIfAbsent(name) { AtomicInteger() }.incrementAndGet()
            if (name.startsWith("ref")) {
                return RecordRef.create("refs", name)
            }
            return "value"
        }
    }
}
