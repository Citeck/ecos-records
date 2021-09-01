package ru.citeck.ecos.records3.test.iter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.iter.IterableRecords
import ru.citeck.ecos.records3.iter.IterableRecordsConfig
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import java.time.Instant

class IterableRecordsDaoTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1

        val indices = mutableListOf<Int>()
        val daoBuilder = RecordsDaoBuilder.create("test")
        repeat(100) { idx ->
            val data = ObjectData.create()
            data.set(RecordConstants.ATT_CREATED, Instant.now().plusMillis(idx.toLong()))
            data.set("index", idx)
            indices.add(idx)
            daoBuilder.addRecord("record-$idx", data)
        }
        records.register(daoBuilder.build())

        var query = RecordsQuery.create {
            withSourceId("test")
            withQuery(VoidPredicate.INSTANCE)
            withSortBy(SortBy(RecordConstants.ATT_CREATED, true))
            withLanguage("predicate")
            withMaxItems(1000)
        }
        val iterableRecords = IterableRecords(
            query,
            IterableRecordsConfig.create {
                withPageSize(10)
                withAttsToLoad(mapOf("index" to "index?num"))
            },
            records
        )

        val indicesFromIterable = mutableListOf<Int>()
        iterableRecords.forEach {
            indicesFromIterable.add(it.getAtt("index").asInt())
        }

        assertThat(indicesFromIterable).isEqualTo(indices)

        query = query.copy { withSortBy(SortBy(RecordConstants.ATT_CREATED, false)) }
        val invIterableRecords = IterableRecords(
            query,
            IterableRecordsConfig.create {
                withPageSize(10)
                withAttsToLoad(mapOf("index" to "index?num"))
            },
            records
        )

        val indicesFromInvIterable = mutableListOf<Int>()
        invIterableRecords.forEach {
            indicesFromInvIterable.add(it.getAtt("index").asInt())
        }

        assertThat(indicesFromInvIterable).isEqualTo(indices.asReversed())
    }
}
