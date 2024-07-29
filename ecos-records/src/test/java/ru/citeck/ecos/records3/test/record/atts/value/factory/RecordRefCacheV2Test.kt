package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.concurrent.atomic.AtomicInteger

class RecordRefCacheV2Test {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsService

        val targetRec = Record(123)
        val targetRecRef = EntityRef.create("target", targetRec.getId())

        val baseRec0 = Record(0, targetRecRef)
        val baseRec1 = Record(1, targetRecRef)
        val baseRec2 = Record(2, targetRecRef)

        val baseRecsRefs = listOf(baseRec0, baseRec1, baseRec2).map {
            EntityRef.create("base", it.getId())
        }

        records.register(
            RecordsDaoBuilder.create("target")
                .addRecord(targetRec.getId(), targetRec)
                .build()
        )
        records.register(
            RecordsDaoBuilder.create("base")
                .addRecord(baseRec0.getId(), baseRec0)
                .addRecord(baseRec1.getId(), baseRec1)
                .addRecord(baseRec2.getId(), baseRec2)
                .build()
        )

        val refValueAtts = mapOf(
            "value" to "ref.value.inner"
        )

        records.getAtts(baseRecsRefs, refValueAtts)

        assertThat(targetRec.getValueCalls.get()).isEqualTo(1)

        records.getAtts(baseRecsRefs, refValueAtts, true)

        assertThat(targetRec.getValueCalls.get()).isEqualTo(2)

        val query = RecordsQuery.create()
            .withSourceId("base")
            .withQuery(Predicates.alwaysTrue())
            .build()

        records.query(query, refValueAtts)

        assertThat(targetRec.getValueCalls.get()).isEqualTo(3)

        records.query(query, refValueAtts, true)

        assertThat(targetRec.getValueCalls.get()).isEqualTo(4)
    }

    class Record(
        private val id: Int,
        private val ref: EntityRef = EntityRef.EMPTY
    ) {

        val getValueCalls = AtomicInteger()
        val getRefCalls = AtomicInteger()

        fun getId(): String {
            return id.toString()
        }

        fun getValue(): Any {
            getValueCalls.incrementAndGet()
            return if (id % 2 == 0) {
                mapOf(
                    "inner" to "value"
                )
            } else {
                "value-$id"
            }
        }

        fun getRef(): EntityRef {
            getRefCalls.incrementAndGet()
            return ref
        }
    }
}
