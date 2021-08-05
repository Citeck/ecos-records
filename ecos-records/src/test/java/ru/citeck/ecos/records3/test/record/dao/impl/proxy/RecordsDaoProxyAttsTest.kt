package ru.citeck.ecos.records3.test.record.dao.impl.proxy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.impl.proxy.AttsProxyProcessor
import ru.citeck.ecos.records3.record.dao.impl.proxy.ProxyRecordAtts
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import kotlin.test.assertEquals

class RecordsDaoProxyAttsTest {

    companion object {
        const val PROXY_ID = "proxy-id"
        const val TARGET_ID = "target-id"
    }

    @Test
    fun attsByIdTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val targetRecordsDao = RecordsDaoBuilder.create(TARGET_ID)
            .addRecord("test", ValueDto())
            .addRecord("linked", LinkedDto())
            .build() as InMemRecordsDao<*>

        records.register(targetRecordsDao)
        records.register(RecordsDaoProxy(PROXY_ID, TARGET_ID))

        val txtId = records.getAtt(RecordRef.create(PROXY_ID, "test"), "?id").asText()
        assertEquals("$PROXY_ID@test", txtId)

        // ================== test query result id ==================

        val query = RecordsQuery.create()
            .withQuery(VoidPredicate.INSTANCE)
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withSourceId(PROXY_ID)
            .build()

        val queryResWithoutAtts = records.query(query)
        assertThat(queryResWithoutAtts.getRecords().map { it.id }).containsAll(targetRecordsDao.records.keys)

        val queryResWithAtts = records.query(query, listOf("strField"))
        assertThat(queryResWithAtts.getRecords().map { it.getId().id }).containsAll(targetRecordsDao.records.keys)

        // ==========================================================

        // test atts with getAtts
        val getAttsById: (sourceId: String, atts: List<String>) -> List<ObjectData> = { sourceId, atts ->
            listOf(records.getAtts(RecordRef.create(sourceId, "test"), atts).getAtts())
        }
        checkFull(getAttsById)

        // test atts with query
        val getAttsByQuery: (sourceId: String, atts: List<String>) -> List<ObjectData> = { sourceId, atts ->
            val queryWithSourceId = RecordsQuery.create()
                .withQuery(Predicates.eq("strField", "str-value"))
                .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                .withSourceId(sourceId)
                .build()
            val res = records.query(queryWithSourceId, atts)
            res.getRecords().map { it.getAtts() }
        }
        checkFull(getAttsByQuery)

        // test atts with proxy processor

        records.unregister(PROXY_ID)
        val proxyWithProc = RecordsDaoProxy(
            PROXY_ID, TARGET_ID,
            object : AttsProxyProcessor {
                override fun attsPreProcess(schemaAtts: List<SchemaAtt>) = schemaAtts
                override fun attsPostProcess(atts: List<ProxyRecordAtts>): List<ProxyRecordAtts> {
                    return atts.map { ProxyRecordAtts(it.atts, mapOf("proc-att" to "proc-value")) }
                }
            }
        )
        records.register(proxyWithProc)

        val procAttValue = records.getAtt(RecordRef.create(PROXY_ID, "test"), "proc-att").asText()
        assertEquals("proc-value", procAttValue)

        val procAttValueWithTarget = records.getAtts(
            RecordRef.create(PROXY_ID, "test"),
            listOf("proc-att", "strField")
        )
        assertEquals("proc-value", procAttValueWithTarget.getAtt("proc-att").asText())
        assertEquals("str-value", procAttValueWithTarget.getAtt("strField").asText())
    }

    private fun checkFull(getAtts: (sourceId: String, atts: List<String>) -> List<ObjectData>) {

        compareAtts(listOf("_type?id"), getAtts)
        compareAtts(listOf("_etype?id"), getAtts)
        // compareAtts(listOf("_type?disp"), getAtts)

        compareAtts(listOf("strField", "unknownField"), getAtts, listOf("unknownField"))
        compareAtts(listOf("unknownField"), getAtts, listOf("unknownField"))
        compareAtts(listOf("linkedDto"), getAtts)

        compareAtts(
            listOf(
                "linkedDto",
                "linkedDto?str",
                "linkedDto.linkedValue",
                "linkedDto.linkedValue?str",
                "linkedDto.linkedValue?disp"
            ),
            getAtts
        )

        compareAtts(
            listOf(
                "linkedRef",
                "linkedRef?str",
                "linkedRef.linkedValue",
                "linkedRef.linkedValue?str",
                "linkedRef.linkedValue?disp",
                "linkedRef.ref?id",
                "linkedRef.ref?assoc",
                // do not add "linkedRef.ref.ref?id"
                "linkedRef.ref.ref?assoc"
            ),
            getAtts
        )
    }

    private fun compareAtts(
        atts: List<String>,
        getAtts: (sourceId: String, atts: List<String>) -> List<ObjectData>,
        nullable: List<String> = emptyList()
    ) {

        val expectedRecordsAtts = getAtts.invoke(TARGET_ID, atts)
        val actual = getAtts.invoke(PROXY_ID, atts)

        assertEquals(expectedRecordsAtts, actual)

        val problemAtts = hashSetOf<String>()
        expectedRecordsAtts.forEach { rec ->
            problemAtts.addAll(atts.filter { !nullable.contains(it) && rec.get(it).isNull() })
        }
        assertEquals(hashSetOf(), problemAtts)
    }

    class ValueDto {
        val strField = "str-value"
        val linkedRef = RecordRef.valueOf("$TARGET_ID@linked")
        val linkedDto = LinkedDto()

        fun getEcosType(): String {
            return "test-type"
        }
    }

    class LinkedDto {

        val linkedValue = "linked-str-value"
        val ref = RecordRef.valueOf("$TARGET_ID@linked")

        fun getDisplayName(): String {
            return "DisplayName"
        }
    }
}
