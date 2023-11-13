package ru.citeck.ecos.records3.test.record.dao.impl.proxy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttEdge
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.impl.proxy.AttsProxyProcessor
import ru.citeck.ecos.records3.record.dao.impl.proxy.ProxyProcContext
import ru.citeck.ecos.records3.record.dao.impl.proxy.ProxyRecordAtts
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.assertEquals

class RecordsDaoProxyAttsTest {

    companion object {
        const val PROXY_ID = "proxy-id"
        const val TARGET_ID = "target-id"
    }

    @Test
    fun edgeAttTest() {

        val records = RecordsServiceFactory().recordsServiceV1

        val targetRecordsDao = RecordsDaoBuilder.create(TARGET_ID)
            .addRecord("att-value-rec", AttValueRec())
            .build() as InMemRecordsDao<*>

        records.register(targetRecordsDao)
        records.register(RecordsDaoProxy(PROXY_ID, TARGET_ID))

        val targetRef = EntityRef.create(TARGET_ID, "att-value-rec")
        val proxyRef = targetRef.withSourceId(PROXY_ID)

        val att = "_edge._status.title.en"
        val targetValue = records.getAtt(targetRef, att).asText()
        val proxyValue = records.getAtt(proxyRef, att).asText()

        assertThat(proxyValue)
            .isEqualTo(targetValue)
            .isEqualTo("status-title")
    }

    @Test
    fun attsByIdTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val targetRecordsDao = RecordsDaoBuilder.create(TARGET_ID)
            .addRecord("test", ValueDto())
            .addRecord("linked", LinkedDto())
            .build() as InMemRecordsDao<*>

        targetRecordsDao.addAttributesMixin(object : AttMixin {
            override fun getAtt(path: String, value: AttValueCtx): Any? {
                return if (path == "mixin-ref") {
                    value.getRef().toString()
                } else {
                    null
                }
            }
            override fun getProvidedAtts(): Collection<String> {
                return listOf("mixin-ref")
            }
        })

        records.register(targetRecordsDao)
        records.register(RecordsDaoProxy(PROXY_ID, TARGET_ID))

        val txtId = records.getAtt(EntityRef.create(PROXY_ID, "test"), "?id").asText()
        assertEquals("$PROXY_ID@test", txtId)

        // ================== test query result id ==================

        val query = RecordsQuery.create()
            .withQuery(VoidPredicate.INSTANCE)
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withSourceId(PROXY_ID)
            .build()

        val queryResWithoutAtts = records.query(query)
        assertThat(queryResWithoutAtts.getRecords().map { it.getLocalId() }).containsAll(targetRecordsDao.records.keys)

        val queryResWithAtts = records.query(query, listOf("strField"))
        assertThat(queryResWithAtts.getRecords().map { it.getId().getLocalId() }).containsAll(targetRecordsDao.records.keys)

        // ==========================================================

        // test atts with getAtts
        val getAttsById: (sourceId: String, atts: List<String>) -> List<ObjectData> = { sourceId, atts ->
            listOf(records.getAtts(EntityRef.create(sourceId, "test"), atts).getAtts())
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
                override fun attsPreProcess(schemaAtts: List<SchemaAtt>, context: ProxyProcContext) = schemaAtts
                override fun attsPostProcess(atts: List<ProxyRecordAtts>, context: ProxyProcContext): List<ProxyRecordAtts> {
                    return atts.map { ProxyRecordAtts(it.atts, mapOf("proc-att" to "proc-value")) }
                }
            }
        )
        records.register(proxyWithProc)

        val procAttValue = records.getAtt(EntityRef.create(PROXY_ID, "test"), "proc-att").asText()
        assertEquals("proc-value", procAttValue)

        val procAttValueWithTarget = records.getAtts(
            EntityRef.create(PROXY_ID, "test"),
            listOf("proc-att", "strField")
        )
        assertEquals("proc-value", procAttValueWithTarget.getAtt("proc-att").asText())
        assertEquals("str-value", procAttValueWithTarget.getAtt("strField").asText())
    }

    private fun checkFull(getAtts: (sourceId: String, atts: List<String>) -> List<ObjectData>) {

        compareAtts(listOf("_raw?raw"), getAtts)
        compareAtts(listOf("_disp"), getAtts)
        compareAtts(listOf("_num?num"), getAtts)
        compareAtts(listOf("_str?str"), getAtts)
        compareAtts(listOf("_bool?bool"), getAtts)
        compareAtts(listOf("_type?id"), getAtts)
        compareAtts(listOf("_type._str?id"), getAtts)
        compareAtts(listOf("_type?localId"), getAtts)
        compareAtts(listOf("_etype?id"), getAtts)
        // compareAtts(listOf("_type?disp"), getAtts)

        compareAtts(listOf("strField", "unknownField"), getAtts, listOf("unknownField"))
        compareAtts(listOf("unknownField"), getAtts, listOf("unknownField"))
        compareAtts(listOf("linkedDto"), getAtts)
        compareAtts(listOf("mixin-ref?id"), getAtts)

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

        val idAttFromProxy = getAtts(PROXY_ID, listOf("?id"))
        for (atts in idAttFromProxy) {
            val ref = EntityRef.valueOf(atts.get("?id").asText())
            assertThat(ref.getSourceId()).isEqualTo(PROXY_ID)
        }
    }

    private fun replaceIdAttsSourceIdToProxySourceId(value: DataValue): DataValue {
        if (value.isArray()) {
            val newValue = DataValue.createArr()
            value.forEach {
                newValue.add(replaceIdAttsSourceIdToProxySourceId(it))
            }
            return newValue
        }
        if (value.isObject()) {
            val newValue = DataValue.createObj()
            value.forEach { key, objValue ->
                if (key.contains("?id") || key.contains("?assoc")) {
                    newValue[key] = replaceIdAttsSourceIdToProxySourceId(objValue)
                } else {
                    newValue[key] = objValue
                }
            }
            return newValue
        }
        if (value.isTextual()) {
            val txt = value.asText()
            return DataValue.createStr(txt.replace("$TARGET_ID@", "$PROXY_ID@"))
        }
        return value
    }

    private fun compareAtts(
        atts: List<String>,
        getAtts: (sourceId: String, atts: List<String>) -> List<ObjectData>,
        nullable: List<String> = emptyList()
    ) {

        val expectedRecordsAtts = getAtts.invoke(TARGET_ID, atts).map {
            ObjectData.create(replaceIdAttsSourceIdToProxySourceId(it.getData()))
        }
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
        val linkedRef = EntityRef.valueOf("$TARGET_ID@linked")
        val linkedDto = LinkedDto()

        fun getEcosType(): String {
            return "test-type"
        }

        fun getDisplayName(): String {
            return "displayName"
        }

        @AttName("?num")
        fun getNum(): Double {
            return 123.123
        }

        @AttName("?str")
        fun getStr(): String {
            return "str-value"
        }

        @AttName("?bool")
        fun getBool(): Boolean {
            return true
        }

        @AttName("?raw")
        fun getRaw(): String {
            return "raw-value-dto"
        }
    }

    class LinkedDto {

        val linkedValue = "linked-str-value"
        val ref = EntityRef.valueOf("$TARGET_ID@linked")

        fun getDisplayName(): String {
            return "DisplayName"
        }
    }

    class AttValueRec : AttValue {

        override fun getEdge(name: String): AttEdge? {
            if (name == "_status") {
                return object : AttEdge {
                    override fun getTitle(): MLText {
                        return MLText("status-title")
                    }
                }
            }
            return null
        }
    }
}
