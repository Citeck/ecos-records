package ru.citeck.ecos.records3.test.op.atts.computed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.*
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.type.RecordTypeService
import kotlin.test.assertEquals

class ComputedStoringTypeTest {

    companion object {

        private const val SOURCE_ID = "test"
        private val TYPE = RecordRef.valueOf("type0")

        const val COMP_VALUE = "computed-value"
    }

    private lateinit var recordsService: RecordsService
    private var attributes: List<ComputedAtt> = emptyList()

    @BeforeEach
    fun before() {

        val services = RecordsServiceFactory()
        services.setRecordTypeService(object : RecordTypeService {
            override fun getComputedAtts(typeRef: RecordRef): List<ComputedAtt> {
                return if (typeRef == TYPE) {
                    attributes
                } else {
                    emptyList()
                }
            }
        })
        recordsService = services.recordsServiceV1
    }

    private fun registerRecords(vararg records: RecordValue): List<RecordRef> {
        val builder = RecordsDaoBuilder.create(SOURCE_ID)
        val refs = mutableListOf<RecordRef>()
        records.forEach { rec ->
            refs.add(RecordRef.create(SOURCE_ID, rec.id))
            builder.addRecord(rec.id, rec)
        }
        recordsService.register(builder.build())
        return refs
    }

    @Test
    fun onEmptyTest() {

        attributes = listOf(
            ComputedAtt(
                "computed",
                ComputedAttDef.create {
                    type = ComputedAttType.VALUE
                    storingType = StoringType.ON_EMPTY
                    config = ObjectData.create(
                        mapOf(
                            Pair("value", COMP_VALUE)
                        )
                    )
                }
            )
        )

        val refs = registerRecords(
            RecordValue("type0Record", TYPE, "first"),
            RecordValue("type1Record", TYPE, ""),
            RecordValue("type2Record", TYPE, null)
        )

        assertEquals("first", recordsService.getAtt(refs[0], "computed").asText())
        RequestContext.doWithCtx {
            ComputedUtils.doWithMutatedRecord {
                assertEquals(COMP_VALUE, recordsService.getAtt(refs[1], "computed").asText())
                assertEquals(COMP_VALUE, recordsService.getAtt(refs[2], "computed").asText())
            }
        }
    }

    @Test
    fun onCreatedAndMutatedTest() {

        val valueFromRecord = "value"
        val record = RecordValue("type0Record", TYPE, valueFromRecord)
        val testRef = registerRecords(record)[0]

        val registerAttsWithStoringType = { st: StoringType ->
            attributes = listOf(
                ComputedAtt(
                    "computed",
                    ComputedAttDef.create {
                        type = ComputedAttType.VALUE
                        storingType = st
                        config = ObjectData.create(
                            mapOf(
                                Pair("value", COMP_VALUE)
                            )
                        )
                    }
                )
            )
        }
        val getCompAtt: () -> String = {
            recordsService.getAtt(testRef, "computed").asText()
        }

        val assertCompAtt = { woCtx: String, withNewRec: String, withMutatedRec: String ->
            RequestContext.doWithCtx {
                assertThat(getCompAtt.invoke()).isEqualTo(woCtx)
                ComputedUtils.doWithNewRecord {
                    assertThat(getCompAtt.invoke()).isEqualTo(withNewRec)
                }
                ComputedUtils.doWithMutatedRecord {
                    assertThat(getCompAtt.invoke()).isEqualTo(withMutatedRec)
                }
            }
        }

        assertCompAtt(valueFromRecord, valueFromRecord, valueFromRecord)

        registerAttsWithStoringType(StoringType.ON_CREATE)
        assertCompAtt(valueFromRecord, COMP_VALUE, valueFromRecord)

        registerAttsWithStoringType(StoringType.ON_MUTATE)
        assertCompAtt(valueFromRecord, COMP_VALUE, COMP_VALUE)

        registerAttsWithStoringType(StoringType.ON_EMPTY)
        record.computed = ""
        assertCompAtt("", COMP_VALUE, COMP_VALUE)
        record.computed = null
        assertCompAtt("", COMP_VALUE, COMP_VALUE)
        record.computed = valueFromRecord
        assertCompAtt(valueFromRecord, valueFromRecord, valueFromRecord)
    }

    data class RecordValue(
        val id: String,
        private val type: RecordRef,
        var computed: String?
    ) {

        @AttName("_type")
        fun getType(): RecordRef {
            return type
        }
    }
}
