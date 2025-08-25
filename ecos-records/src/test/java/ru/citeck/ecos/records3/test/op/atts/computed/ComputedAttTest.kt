package ru.citeck.ecos.records3.test.op.atts.computed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttResType
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttType
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttValue
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.type.RecordTypeComponent
import ru.citeck.ecos.records3.record.type.RecordTypeInfo
import ru.citeck.ecos.records3.record.type.RecordTypeInfoAdapter
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

class ComputedAttTest {

    companion object {
        val type0 = EntityRef.valueOf("type0")
        val type1 = EntityRef.valueOf("type1")
    }

    @Test
    fun test() {

        val type0Atts = listOf(
            RecordComputedAtt.create {
                withId("attAttribute")
                withType(RecordComputedAttType.ATTRIBUTE)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair("attribute", "attAttributeValue")
                        )
                    )
                )
            },
            RecordComputedAtt.create {
                withId("attScript0")
                withType(RecordComputedAttType.SCRIPT)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load('attForScript');")
                        )
                    )
                )
            },
            RecordComputedAtt.create {
                withId("attScript1")
                withType(RecordComputedAttType.SCRIPT)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load({'key':'attForScript'});")
                        )
                    )
                )
            },
            RecordComputedAtt.create {
                withId("attScript2")
                withType(RecordComputedAttType.SCRIPT)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load(['attForScript']);")
                        )
                    )
                )
            },
            RecordComputedAtt.create {
                withId("attScript3")
                withType(RecordComputedAttType.SCRIPT)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load('attForScript') + '-postfix';")
                        )
                    )
                )
            },
            RecordComputedAtt.create {
                withId("attScript4")
                withType(RecordComputedAttType.SCRIPT)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair("fn", "return value.load('attForScript') + '-postfix';")
                        )
                    )
                )
            },
            RecordComputedAtt.create {
                withId("attScript5")
                withType(RecordComputedAttType.SCRIPT)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair("fn", "return [value.load('attForScript'), 'def'];")
                        )
                    )
                )
            },
            RecordComputedAtt.create {
                withId("attPersonValueRef")
                withType(RecordComputedAttType.VALUE)
                withResultType(RecordComputedAttResType.AUTHORITY)
                withConfig(ObjectData.create().set("value", "emodel/person@system"))
            },
            RecordComputedAtt.create {
                withId("attGroupValueRef")
                withType(RecordComputedAttType.VALUE)
                withResultType(RecordComputedAttResType.AUTHORITY)
                withConfig(ObjectData.create().set("value", "emodel/authority-group@some-group"))
            },
            RecordComputedAtt.create {
                withId("attPersonValueAuthName")
                withType(RecordComputedAttType.VALUE)
                withResultType(RecordComputedAttResType.AUTHORITY)
                withConfig(ObjectData.create().set("value", "system"))
            },
            RecordComputedAtt.create {
                withId("attGroupValueAuthName")
                withType(RecordComputedAttType.VALUE)
                withResultType(RecordComputedAttResType.AUTHORITY)
                withConfig(ObjectData.create().set("value", "GROUP_some-group"))
            }
        )

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock()
            }
        }
        services.setRecordTypeComponent(object : RecordTypeComponent {
            override fun getRecordType(typeRef: EntityRef): RecordTypeInfo? {
                return if (typeRef == type0) {
                    object : RecordTypeInfoAdapter() {
                        override fun getComputedAtts() = type0Atts
                    }
                } else {
                    null
                }
            }
        })

        val type0Record = RecordValue("type0Record", type0, "first", "second")
        val type1Record = RecordValue("type1Record", type1, "third", "fourth")

        val type0Ref = EntityRef.create("test", type0Record.id)
        val type1Ref = EntityRef.create("test", type1Record.id)

        services.recordsService.register(
            RecordsDaoBuilder.create("test")
                .addRecord(type0Record.id, type0Record)
                .addRecord(type1Record.id, type1Record)
                .build()
        )
        //
        assertEquals(
            DataValue.create("{\"attForScript\":\"${type0Record.attForScript}\"}"),
            services.recordsService.getAtt(type0Ref, "attScript2?json")
        )
        //

        assertEquals(
            DataValue.create(type0Record.attAttributeValue),
            services.recordsService.getAtt(type0Ref, "attAttribute")
        )

        assertEquals(
            DataValue.create(type0Record.attForScript),
            services.recordsService.getAtt(type0Ref, "attScript0")
        )
        assertEquals(
            DataValue.create("{\"key\":\"${type0Record.attForScript}\"}"),
            services.recordsService.getAtt(type0Ref, "attScript1?json")
        )
        assertEquals(
            DataValue.create("{\"attForScript\":\"${type0Record.attForScript}\"}"),
            services.recordsService.getAtt(type0Ref, "attScript2?json")
        )
        assertEquals(
            type0Record.attForScript + "-postfix",
            services.recordsService.getAtt(type0Ref, "attScript3").asText()
        )
        assertEquals(
            type0Record.attForScript + "-postfix",
            services.recordsService.getAtt(type0Ref, "attScript4").asText()
        )
        assertEquals(
            DataValue.create(type0Record.attForScript),
            services.recordsService.getAtt(type0Ref, "attScript5")
        )
        assertEquals(
            DataValue.create("[\"${type0Record.attForScript}\",\"def\"]"),
            services.recordsService.getAtt(type0Ref, "attScript5[]")
        )

        val records = services.recordsService
        assertThat(records.getAtt(type0Ref, "attPersonValueRef?raw"))
            .isEqualTo(DataValue.createStr("emodel/person@system"))
        assertThat(records.getAtt(type0Ref, "attPersonValueAuthName?raw"))
            .isEqualTo(DataValue.createStr("emodel/person@system"))

        assertThat(records.getAtt(type0Ref, "attGroupValueRef?raw"))
            .isEqualTo(DataValue.createStr("emodel/authority-group@some-group"))
        assertThat(records.getAtt(type0Ref, "attGroupValueAuthName?raw"))
            .isEqualTo(DataValue.createStr("emodel/authority-group@some-group"))
    }

    @Test
    fun contextComputedAttTest() {

        val records = RecordsServiceFactory().recordsService

        val computedAttDef0 = RecordComputedAttValue.create()
            .withType(RecordComputedAttType.SCRIPT)
            .withConfig(
                ObjectData.create(
                    """
                {
                    "fn": "return 'abc-' + value.load('prop');"
                }
                    """.trimIndent()
                )
            )
            .build()

        val computedAttDef1 = RecordComputedAttValue.create()
            .withType(RecordComputedAttType.SCRIPT)
            .withConfig(
                ObjectData.create(
                    """
                {
                    "fn": "return 'abc-' + value.load({alias: 'prop'}).alias;"
                }
                    """.trimIndent()
                )
            )
            .build()

        val computedAttDef2 = RecordComputedAttValue.create()
            .withType(RecordComputedAttType.SCRIPT)
            .withConfig(
                ObjectData.create(
                    """
                {
                    "fn": "return 'abc-' + value.load(['prop']).prop;"
                }
                    """.trimIndent()
                )
            )
            .build()

        val recordData = ObjectData.create(
            """
            {"prop": "prop-value"}
            """.trimIndent()
        )

        listOf(computedAttDef0, computedAttDef1, computedAttDef2).forEach { compAtt ->
            val result = RequestContext.doWithAtts(mapOf("comp" to compAtt)) { _ ->
                records.getAtt(recordData, "\$comp").asText()
            }
            assertThat(result).isEqualTo("abc-prop-value")
        }
    }

    @Test
    fun testWithPostProc() {

        val computedAttDef1 = RecordComputedAttValue.create()
            .withType(RecordComputedAttType.TEMPLATE)
            .withConfig(
                ObjectData.create("""{ "template": "${"$"}{${"$"}now|fmt('yyyy')}" }""")
            )
            .build()

        val computedAttDef2 = RecordComputedAttValue.create()
            .withType(RecordComputedAttType.TEMPLATE)
            .withConfig(
                ObjectData.create("""{ "template": "${"$"}{${"$"}now|fmt('yyyy')!'1234'}" }""")
            )
            .build()

        val currentYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR)

        listOf(computedAttDef1, computedAttDef2).forEach { compAtt ->
            val records = RecordsServiceFactory().recordsService
            val result = RequestContext.doWithAtts(mapOf("comp" to compAtt)) { _ ->
                records.getAtt(null, "\$comp").asInt()
            }
            assertThat(result).isEqualTo(currentYear)
        }
    }

    data class RecordValue(
        val id: String,
        private val type: EntityRef,
        val attAttributeValue: String,
        val attForScript: String
    ) {

        @AttName("_type")
        fun getType(): EntityRef {
            return type
        }
    }
}
