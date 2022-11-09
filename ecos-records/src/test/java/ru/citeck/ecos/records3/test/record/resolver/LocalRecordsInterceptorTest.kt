package ru.citeck.ecos.records3.test.record.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.exception.RecordsSourceNotFoundException
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.resolver.interceptor.*
import ru.citeck.ecos.webapp.api.entity.EntityRef

class LocalRecordsInterceptorTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        services.recordsServiceV1.register(
            RecordsDaoBuilder.create("test")
                .addRecord("test", TestRecord("abc", 123))
                .addRecord("test2", TestRecord("def", 456))
                .build()
        )

        val queryCalls = ArrayList<RecordsQuery>()
        val getValueAttsCalls = ArrayList<List<*>>()
        val getRecordAttsCalls = ArrayList<List<EntityRef>>()
        val mutateCalls = ArrayList<RecordAtts>()
        val deleteCalls = ArrayList<List<EntityRef>>()

        services.localRecordsResolver.addInterceptor(object : LocalRecordsInterceptor {
            override fun queryRecords(
                queryArg: RecordsQuery,
                attributes: List<SchemaAtt>,
                rawAtts: Boolean,
                chain: QueryRecordsInterceptorsChain
            ): RecsQueryRes<RecordAtts> {
                queryCalls.add(queryArg)
                return chain.invoke(queryArg, attributes, rawAtts)
            }

            override fun getValuesAtts(
                values: List<*>,
                attributes: List<SchemaAtt>,
                rawAtts: Boolean,
                chain: GetValuesAttsInterceptorsChain
            ): List<RecordAtts> {
                getValueAttsCalls.add(values)
                return chain.invoke(values, attributes, rawAtts)
            }

            override fun getRecordsAtts(
                sourceId: String,
                recordIds: List<String>,
                attributes: List<SchemaAtt>,
                rawAtts: Boolean,
                chain: GetRecordsAttsInterceptorsChain
            ): List<RecordAtts> {
                getRecordAttsCalls.add(recordIds.map { EntityRef.create(sourceId, it) })
                return chain.invoke(sourceId, recordIds, attributes, rawAtts)
            }

            override fun mutateRecord(
                sourceId: String,
                record: LocalRecordAtts,
                attsToLoad: List<SchemaAtt>,
                rawAtts: Boolean,
                chain: MutateRecordInterceptorsChain
            ): RecordAtts {
                mutateCalls.add(RecordAtts(RecordRef.create(sourceId, record.id), record.attributes))
                return chain.invoke(sourceId, record, attsToLoad, rawAtts)
            }

            override fun deleteRecords(sourceId: String, recordIds: List<String>, chain: DeleteRecordsInterceptorsChain): List<DelStatus> {
                deleteCalls.add(recordIds.map { RecordRef.create(sourceId, it) })
                return chain.invoke(sourceId, recordIds)
            }
        })

        val records = services.recordsServiceV1

        val queryResult = records.query(
            RecordsQuery.create {
                withSourceId("test")
                withQuery(VoidPredicate.INSTANCE)
            }
        )

        assertThat(queryCalls).hasSize(1)
        assertThat(queryResult.getRecords()).hasSize(2)
        assertThat(queryResult.getRecords()).containsExactlyInAnyOrderElementsOf(
            listOf("test", "test2").map {
                RecordRef.create("test", it)
            }
        )
        assertThat(getRecordAttsCalls).isEmpty()
        assertThat(getValueAttsCalls).isEmpty()
        assertThat(mutateCalls).isEmpty()
        assertThat(deleteCalls).isEmpty()

        val getAttRes = records.getAtt("test@test", "str").asText()
        assertThat(getAttRes).isEqualTo("abc")

        assertThat(getRecordAttsCalls).hasSize(1)
        assertThat(getRecordAttsCalls).containsExactly(listOf(RecordRef.valueOf("test@test")))

        try {
            records.delete("test@test")
        } catch (e: RecordsSourceNotFoundException) {
            // do nothing
        }
        assertThat(deleteCalls).hasSize(1)
        try {
            records.mutate("test@test", mapOf("str" to "value"))
        } catch (e: RecordsSourceNotFoundException) {
            // do nothing
        }
        assertThat(mutateCalls).hasSize(1)
    }

    class TestRecord(val str: String, val num: Int)
}
