package ru.citeck.ecos.records3.test.record.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.exception.RecsSourceNotFoundException
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.resolver.interceptor.*

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
        val getAttsCalls = ArrayList<List<*>>()
        val mutateCalls = ArrayList<List<RecordAtts>>()
        val deleteCalls = ArrayList<List<RecordRef>>()

        services.localRecordsResolver.addInterceptor(object : LocalRecordsInterceptor {
            override fun query(
                queryArg: RecordsQuery,
                attributes: List<SchemaAtt>,
                rawAtts: Boolean,
                chain: QueryInterceptorsChain
            ): RecsQueryRes<RecordAtts> {
                queryCalls.add(queryArg)
                return chain.invoke(queryArg, attributes, rawAtts)
            }

            override fun getAtts(
                records: List<*>,
                attributes: List<SchemaAtt>,
                rawAtts: Boolean,
                chain: GetAttsInterceptorsChain
            ): List<RecordAtts> {
                getAttsCalls.add(records)
                return chain.invoke(records, attributes, rawAtts)
            }

            override fun mutate(
                records: List<RecordAtts>,
                attsToLoad: List<SchemaAtt>,
                rawAtts: Boolean,
                chain: MutateInterceptorsChain
            ): List<RecordAtts> {
                mutateCalls.add(records)
                return chain.invoke(records, attsToLoad, rawAtts)
            }

            override fun delete(records: List<RecordRef>, chain: DeleteInterceptorsChain): List<DelStatus> {
                deleteCalls.add(records)
                return chain.invoke(records)
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
        assertThat(getAttsCalls).isEmpty()
        assertThat(mutateCalls).isEmpty()
        assertThat(deleteCalls).isEmpty()

        val getAttRes = records.getAtt("test@test", "str").asText()
        assertThat(getAttRes).isEqualTo("abc")

        assertThat(getAttsCalls).hasSize(1)
        assertThat(getAttsCalls).containsExactly(listOf(RecordRef.valueOf("test@test")))

        try {
            records.delete("test@test")
        } catch (e: RecsSourceNotFoundException) {
            // do nothing
        }
        assertThat(deleteCalls).hasSize(1)
        try {
            records.mutate("test@test", mapOf("str" to "value"))
        } catch (e: RecsSourceNotFoundException) {
            // do nothing
        }
        assertThat(mutateCalls).hasSize(1)
    }

    class TestRecord(val str: String, val num: Int)
}
