package ru.citeck.ecos.records3.test.iter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.iter.IterableRecords
import ru.citeck.ecos.records3.iter.IterableRecordsConfig
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import java.time.Instant

class IterableRecordsDaoTest {

    @ValueSource(ints = [2, 10, 13, 50, 55, 1000])
    @ParameterizedTest
    fun test(batchSize: Int) {

        val records = RecordsServiceFactory().recordsServiceV1

        val indices = mutableListOf<Int>()
        val daoBuilder = RecordsDaoBuilder.create("test")
        repeat(123) { idx ->
            val data = ObjectData.create()
            data[RecordConstants.ATT_CREATED] = Instant.now().plusMillis(idx.toLong())
            data["index"] = idx
            indices.add(idx)
            daoBuilder.addRecord("record-$idx", data)
        }
        val reversedIndices = ArrayList(indices).asReversed()

        records.register(daoBuilder.build())

        fun testForEach(forEachMethod: (() -> IterableRecords, (RecordAtts) -> Unit) -> Unit) {

            var query = RecordsQuery.create {
                withSourceId("test")
                withQuery(VoidPredicate.INSTANCE)
                withSortBy(SortBy(RecordConstants.ATT_CREATED, true))
                withLanguage("predicate")
                withMaxItems(1000)
            }

            val createIterableRecords: () -> IterableRecords = {
                IterableRecords(
                    query,
                    IterableRecordsConfig.create {
                        withPageSize(batchSize)
                        withAttsToLoad(mapOf("index" to "index?num"))
                    },
                    records
                )
            }

            val indicesFromIterable = mutableListOf<Int>()
            forEachMethod(createIterableRecords) {
                indicesFromIterable.add(it.getAtt("index").asInt())
            }

            assertThat(indicesFromIterable).isEqualTo(indices)

            query = query.copy { withSortBy(SortBy(RecordConstants.ATT_CREATED, false)) }
            val createInvIterableRecords: () -> IterableRecords = {
                IterableRecords(
                    query,
                    IterableRecordsConfig.create {
                        withPageSize(batchSize)
                        withAttsToLoad(mapOf("index" to "index?num"))
                    },
                    records
                )
            }

            val indicesFromInvIterable = mutableListOf<Int>()
            forEachMethod(createInvIterableRecords) {
                indicesFromInvIterable.add(it.getAtt("index").asInt())
            }

            assertThat(indicesFromInvIterable).isEqualTo(reversedIndices)
        }

        testForEach { recordsIt, action ->
            recordsIt().forEach(action)
        }

        fun testWithState(full: Boolean) {
            testForEach { recordsIt, action ->
                var state = ObjectData.create()
                for (i in 0..1000) {
                    val iterator = recordsIt().iterator()
                    iterator.setState(state)
                    var iterationCompleted = false
                    for (idx in 0 until batchSize) {
                        if (!iterator.hasNext()) {
                            iterationCompleted = true
                            break
                        }
                        action.invoke(iterator.next())
                    }
                    state = iterator.getState(full)
                    if (iterationCompleted) {
                        break
                    }
                }
            }
        }
        testWithState(true)
        testWithState(false)
    }
}
