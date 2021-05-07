package ru.citeck.ecos.records3.test.record.dao.impl.proxy

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.proxy.AttsProxyProcessor
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordsDaoProxyAttsTest {

    companion object {
        const val PROXY_ID = "proxy-id"
        const val TARGET_ID = "target-id"
    }

    @Test
    fun attsByIdTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        records.register(
            RecordsDaoBuilder.create(TARGET_ID)
                .addRecord("test", ValueDto())
                .addRecord("linked", LinkedDto())
                .build()
        )
        records.register(RecordsDaoProxy(PROXY_ID, TARGET_ID))

        val txtId = records.getAtt(RecordRef.create(PROXY_ID, "test"), "?id").asText()
        assertEquals("$PROXY_ID@test", txtId)

        // test atts with getAtts
        val getAttsById: (sourceId: String, atts: List<String>) -> List<ObjectData> = { sourceId, atts ->
            listOf(records.getAtts(RecordRef.create(sourceId, "test"), atts).getAtts())
        }
        checkFull(getAttsById)

        // test atts with query
        val getAttsByQuery: (sourceId: String, atts: List<String>) -> List<ObjectData> = { sourceId, atts ->
            val query = RecordsQuery.create()
                .withQuery(Predicates.eq("strField", "str-value"))
                .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                .withSourceId(sourceId)
                .build()
            val res = records.query(query, atts)
            res.getRecords().map { it.getAtts() }
        }
        checkFull(getAttsByQuery)

        // test atts with proxy processor

        records.unregister(PROXY_ID)
        val proxyWithProc = RecordsDaoProxy(
            PROXY_ID, TARGET_ID,
            object : AttsProxyProcessor {
                override fun prepareAtts(atts: MutableMap<String, String>) {
                }

                override fun postProcessAtts(atts: List<RecordAtts>): List<Map<String, Any>> {
                    return atts.map { mapOf("proc-att" to "proc-value") }
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

        compareAtts(listOf("strField", "unknownField"), getAtts, listOf("unknownField"))
        compareAtts(listOf("unknownField"), getAtts, listOf("unknownField"))
        compareAtts(listOf("linkedDto"), getAtts)

        listOf(
            "linkedDto",
            "linkedDto?str",
            "linkedDto.linkedValue",
            "linkedDto.linkedValue?str",
            "linkedDto.linkedValue?disp"
        ).forEach {
            compareAtts(listOf(it), getAtts)
        }

        listOf(
            "linkedRef",
            "linkedRef?str",
            "linkedRef.linkedValue",
            "linkedRef.linkedValue?str",
            "linkedRef.linkedValue?disp"
        ).forEach {
            compareAtts(listOf(it), getAtts)
        }
    }

    private fun compareAtts(
        atts: List<String>,
        getAtts: (sourceId: String, atts: List<String>) -> List<ObjectData>,
        nullable: List<String> = emptyList()
    ) {

        val expected = getAtts.invoke(TARGET_ID, atts)
        val actual = getAtts.invoke(PROXY_ID, atts)

        assertEquals(expected, actual)
        assertTrue {
            expected.all { rec ->
                atts.all {
                    nullable.contains(it) || rec.get(it).isNotNull()
                }
            }
        }
    }

    class ValueDto {
        val strField = "str-value"
        val linkedRef = RecordRef.valueOf("$TARGET_ID@linked")
        val linkedDto = LinkedDto()
    }

    class LinkedDto {

        val linkedValue = "linked-str-value"

        fun getDisplayName(): String {
            return "DisplayName"
        }
    }
}
