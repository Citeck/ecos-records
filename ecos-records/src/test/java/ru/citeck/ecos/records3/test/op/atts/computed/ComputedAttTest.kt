package ru.citeck.ecos.records3.test.op.atts.computed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.ComputedAtt
import ru.citeck.ecos.records3.record.atts.computed.ComputedAttDef
import ru.citeck.ecos.records3.record.atts.computed.ComputedAttType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.type.RecordTypeService
import kotlin.test.assertEquals

class ComputedAttTest {

    companion object {
        val type0 = RecordRef.valueOf("type0")
        val type1 = RecordRef.valueOf("type1")
    }

    @Test
    fun test() {

        val type0Atts = listOf(
            ComputedAtt(
                "attAttribute",
                ComputedAttDef.create {
                    type = ComputedAttType.ATTRIBUTE
                    config = ObjectData.create(
                        mapOf(
                            Pair("attribute", "attAttributeValue")
                        )
                    )
                }
            ),
            ComputedAtt(
                "attScript0",
                ComputedAttDef.create {
                    type = ComputedAttType.SCRIPT
                    config = ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load('attForScript');")
                        )
                    )
                }
            ),
            ComputedAtt(
                "attScript1",
                ComputedAttDef.create {
                    type = ComputedAttType.SCRIPT
                    config = ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load({'key':'attForScript'});")
                        )
                    )
                }
            ),
            ComputedAtt(
                "attScript2",
                ComputedAttDef.create {
                    type = ComputedAttType.SCRIPT
                    config = ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load(['attForScript']);")
                        )
                    )
                }
            ),
            ComputedAtt(
                "attScript3",
                ComputedAttDef.create {
                    type = ComputedAttType.SCRIPT
                    config = ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load('attForScript') + '-postfix';")
                        )
                    )
                }
            ),
            ComputedAtt(
                "attScript4",
                ComputedAttDef.create {
                    type = ComputedAttType.SCRIPT
                    config = ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load('attForScript').asText() + '-postfix';")
                        )
                    )
                }
            ),
            ComputedAtt(
                "attScript5",
                ComputedAttDef.create {
                    type = ComputedAttType.SCRIPT
                    config = ObjectData.create(
                        mapOf(
                            Pair("fn", "return [value.load('attForScript'), 'def'];")
                        )
                    )
                }
            )
        )

        val services = RecordsServiceFactory()
        services.setRecordTypeService(object : RecordTypeService {
            override fun getComputedAtts(typeRef: RecordRef): List<ComputedAtt> {
                return if (typeRef == type0) {
                    type0Atts
                } else {
                    emptyList()
                }
            }
        })

        val type0Record = RecordValue("type0Record", type0, "first", "second")
        val type1Record = RecordValue("type1Record", type1, "third", "fourth")

        val type0Ref = RecordRef.create("test", type0Record.id)
        val type1Ref = RecordRef.create("test", type1Record.id)

        services.recordsServiceV1.register(
            RecordsDaoBuilder.create("test")
                .addRecord(type0Record.id, type0Record)
                .addRecord(type1Record.id, type1Record)
                .build()
        )
        //
        assertEquals(
            DataValue.create("{\"attForScript\":\"${type0Record.attForScript}\"}"),
            services.recordsServiceV1.getAtt(type0Ref, "attScript2?json")
        )
        //

        assertEquals(DataValue.create(type0Record.attAttributeValue), services.recordsServiceV1.getAtt(type0Ref, "attAttribute"))

        assertEquals(DataValue.create(type0Record.attForScript), services.recordsServiceV1.getAtt(type0Ref, "attScript0"))
        assertEquals(
            DataValue.create("{\"key\":\"${type0Record.attForScript}\"}"),
            services.recordsServiceV1.getAtt(type0Ref, "attScript1?json")
        )
        assertEquals(
            DataValue.create("{\"attForScript\":\"${type0Record.attForScript}\"}"),
            services.recordsServiceV1.getAtt(type0Ref, "attScript2?json")
        )
        assertEquals(
            '"' + type0Record.attForScript + '"' + "-postfix",
            services.recordsServiceV1.getAtt(type0Ref, "attScript3").asText()
        )
        assertEquals(
            type0Record.attForScript + "-postfix",
            services.recordsServiceV1.getAtt(type0Ref, "attScript4").asText()
        )
        assertEquals(
            DataValue.create(type0Record.attForScript),
            services.recordsServiceV1.getAtt(type0Ref, "attScript5")
        )
        assertEquals(
            DataValue.create("[\"${type0Record.attForScript}\",\"def\"]"),
            services.recordsServiceV1.getAtt(type0Ref, "attScript5[]")
        )
    }

    @Test
    fun contextComputedAttTest() {

        val records = RecordsServiceFactory().recordsServiceV1

        val computedAttDef0 = ComputedAttDef.create()
            .withType(ComputedAttType.SCRIPT)
            .withConfig(ObjectData.create("""
                {
                    "fn": "return 'abc-' + value.load('prop');"
                }
            """.trimIndent()))
            .build()

        val computedAttDef1 = ComputedAttDef.create()
            .withType(ComputedAttType.SCRIPT)
            .withConfig(ObjectData.create("""
                {
                    "fn": "return 'abc-' + value.load({alias: 'prop'}).alias;"
                }
            """.trimIndent()))
            .build()

        val computedAttDef2 = ComputedAttDef.create()
            .withType(ComputedAttType.SCRIPT)
            .withConfig(ObjectData.create("""
                {
                    "fn": "return 'abc-' + value.load(['prop']).prop;"
                }
            """.trimIndent()))
            .build()

        val recordData = ObjectData.create("""
            {"prop": "prop-value"}
        """.trimIndent())

        listOf(computedAttDef0, computedAttDef1, computedAttDef2).forEach { compAtt ->
            val result = RequestContext.doWithAtts(mapOf("comp" to compAtt)) { _ ->
                records.getAtt(recordData, "\$comp").asText()
            }
            assertThat(result).isEqualTo("abc-prop-value")
        }
    }

    data class RecordValue(
        val id: String,
        private val type: RecordRef,
        val attAttributeValue: String,
        val attForScript: String
    ) {

        @AttName("_type")
        fun getType(): RecordRef {
            return type
        }
    }
}
