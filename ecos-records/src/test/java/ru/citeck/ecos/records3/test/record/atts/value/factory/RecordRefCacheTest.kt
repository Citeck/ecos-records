package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.concurrent.atomic.AtomicInteger

class RecordRefCacheTest {

    @Test
    fun complexTest() {

        val record0 = RecordWithCounters(0)
        val record1 = RecordWithCounters(1)
        val record2 = RecordWithCounters(2)

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("rec-0", record0)
                .addRecord("rec-1", record1)
                .addRecord("rec-2", record2)
                .build()
        )

        records.register(object : RecordsQueryDao {
            override fun getId() = "test-2"
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                val recs = listOf("test@rec-0", "test@rec-1", "test@rec-2")
                val recsVariants = listOf(
                    listOf(recs[0]),
                    listOf(recs[0], recs[1]),
                    listOf(recs[0], recs[1], recs[2]),
                    listOf(recs[0]),
                    listOf(recs[1]),
                    listOf(recs[2]),
                    listOf(recs[2], recs[1], recs[0]),
                    listOf(recs[0], recs[0], recs[0]),
                    listOf(recs[0], recs[1], recs[0]),
                    listOf(recs[2], recs[1], recs[2])
                )
                recsVariants.forEach { recsToTest ->
                    val atts0 = records.getAtts(recsToTest, ComplexTestAtts0::class.java)
                    for (recIdx in recsToTest.indices) {
                        assertThat(atts0[recIdx].perms)
                            .describedAs("recs: $recsToTest")
                            .isEqualTo(recsToTest[recIdx].endsWith("-1"))
                        assertThat(atts0[recIdx].value)
                            .describedAs("recs: $recsToTest")
                            .isEqualTo("value-" + recsToTest[recIdx].substringAfterLast('-'))
                    }
                    val atts1 = records.getAtts(recsToTest, ComplexTestAtts1::class.java)
                    for (recIdx in recsToTest.indices) {
                        assertThat(atts1[recIdx].perms).isEqualTo(recsToTest[recIdx].endsWith("-2"))
                        assertThat(atts1[recIdx].value1).isEqualTo(
                            "value-" + recsToTest[recIdx].substringAfterLast(
                                '-'
                            )
                        )
                    }
                }
                return emptyList<Any>()
            }
        })

        records.query(
            RecordsQuery.create()
                .withSourceId("test-2")
                .build()
        )

        // see comment in LocalRemoteResolver:~218 to understand why all values is not 1
        assertThat(record0.getValueCounter.get()).isLessThanOrEqualTo(3)
        assertThat(record1.getValueCounter.get()).isLessThanOrEqualTo(2)
        assertThat(record2.getValueCounter.get()).isLessThanOrEqualTo(1)
    }

    class ComplexTestAtts0(
        val value: String,
        @AttName("permissions._has.1?bool")
        val perms: Boolean
    )
    class ComplexTestAtts1(
        @AttName("value")
        val value1: String,
        @AttName("permissions._has.2?bool")
        val perms: Boolean
    )

    class RecordWithCounters(private val idx: Int) {

        val getValueCounter = AtomicInteger()

        fun getValue(): String {
            getValueCounter.incrementAndGet()
            return "value-$idx"
        }

        fun getPermissions(): Any {
            return Perms()
        }

        inner class Perms : AttValue {
            override fun has(name: String): Boolean {
                return name == idx.toString()
            }
        }
    }

    @Test
    fun testWithQuery() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(object : RecordsQueryDao, RecordAttsDao {
            override fun getId() = "test"
            override fun getRecordAtts(recordId: String): Any {
                return EntityRef.create("refs", recordId)
            }
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                return listOf("ref0", "ref1", "ref2").map {
                    mapOf("ref" to EntityRef.create("refs", it))
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

        RequestContext.doWithCtx({ d -> d.withReadOnly(true) }) {

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
                return EntityRef.create("refs", recordId)
            }
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                return listOf("ref0", "ref1", "ref2").map {
                    mapOf("ref" to EntityRef.create("refs", it))
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
                return EntityRef.create("refs", name)
            }
            if (name.startsWith("str")) {
                return name
            }
            return "value"
        }
    }
}
