package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.concurrent.atomic.AtomicInteger

class RecordRefCacheTest {

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

        RequestContext.doWithCtx {

            // get atts test

            val atts = records.getAtts(
                "refs@some-test-value",
                mapOf(
                    "first" to "value",
                    "second" to "ref-1234.str-123",
                    "second-2" to "ref-1234.str-123",
                    "third" to "ref-1234.str-456",
                    "fourth" to "str-555"
                )
            )
            assertThat(atts.getAtt("first").asText()).isEqualTo("value")
            assertThat(atts.getAtt("second").asText()).isEqualTo("str-123")
            assertThat(atts.getAtt("second-2").asText()).isEqualTo("str-123")
            assertThat(atts.getAtt("third").asText()).isEqualTo("str-456")
            assertThat(atts.getAtt("fourth").asText()).isEqualTo("str-555")

            // get atts with same ref and att test

            val id = "refs@same-rec-and-att-test"

            assertThat(records.getAtt(id, "str-456").asText()).isEqualTo("str-456")
            assertThat(records.getAtt(id, "str-456").asText()).isEqualTo("str-456")
            assertThat(records.getAtt(id, "str-456").asText()).isEqualTo("str-456")
            assertThat(getAttsCounters[id.substringAfterLast('@')]!!.get()).isEqualTo(1)

            assertThat(records.getAtt(id, "str-4567").asText()).isEqualTo("str-4567")
            assertThat(records.getAtt(id, "str-4567").asText()).isEqualTo("str-4567")
            assertThat(records.getAtt(id, "str-4567").asText()).isEqualTo("str-4567")
            assertThat(getAttsCounters[id.substringAfterLast('@')]!!.get()).isEqualTo(2)
        }
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
            if (name.startsWith("str")) {
                return name
            }
            return "value"
        }
    }
}
