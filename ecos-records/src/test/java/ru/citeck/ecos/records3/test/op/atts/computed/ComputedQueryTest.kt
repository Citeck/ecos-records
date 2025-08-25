package ru.citeck.ecos.records3.test.op.atts.computed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.type.RecordTypeComponent
import ru.citeck.ecos.records3.record.type.RecordTypeInfo
import ru.citeck.ecos.records3.record.type.RecordTypeInfoAdapter
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ComputedQueryTest {

    companion object {
        val type0 = EntityRef.valueOf("type0")
    }

    @Test
    fun test() {

        val type0Atts = listOf(
            RecordComputedAtt.create {
                withId("attScript0")
                withType(RecordComputedAttType.SCRIPT)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair(
                                "fn",
                                """
                                var queryRes = Records.query({
                                    sourceId: 'test',
                                    query: {
                                        t: 'eq',
                                        att: 'attAttributeValue',
                                        val: 'first'
                                    },
                                    language: 'predicate'
                                }, {
                                    'att': 'attForScript'
                                });
                                return queryRes.records[0];
                                """.trimIndent()
                            )
                        )
                    )
                )
            },
            RecordComputedAtt.create {
                withId("attScript01")
                withType(RecordComputedAttType.SCRIPT)
                withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair(
                                "fn",
                                """
                                var queryRes = Records.query({
                                    sourceId: 'test',
                                    query: {
                                        t: 'eq',
                                        att: 'attAttributeValue',
                                        val: 'first'
                                    },
                                    language: 'predicate'
                                }, {
                                    'att': 'attForScript'
                                });
                                return queryRes.records.map(function(rec) { return rec.att; });
                                """.trimIndent()
                            )
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
                            Pair(
                                "fn",
                                """
                                return Records.get('test@type0Record').load('attAttributeValue');
                                """.trimIndent()
                            )
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
                            Pair(
                                "fn",
                                """
                                return Records.get('test@type0Record').load(['attAttributeValue']);
                                """.trimIndent()
                            )
                        )
                    )
                )
            }
        )

        val services = RecordsServiceFactory()
        services.setRecordTypeComponent(object : RecordTypeComponent {
            override fun getRecordType(typeRef: EntityRef): RecordTypeInfo? {
                return if (typeRef == type0) {
                    object : RecordTypeInfoAdapter() {
                        override fun getComputedAtts(): List<RecordComputedAtt> = type0Atts
                    }
                } else {
                    null
                }
            }
        })

        val type0Record = RecordValue("type0Record", type0, "first", "second")
        val type0Ref = EntityRef.create("test", type0Record.id)

        services.recordsService.register(
            RecordsDaoBuilder.create("test")
                .addRecord(type0Record.id, type0Record)
                .build()
        )

        assertEquals(
            DataValue.create(
                "{" +
                    "\"id\":\"test@type0Record\"," +
                    "\"att\":\"${type0Record.attForScript}\"" +
                    "}"
            ),
            services.recordsService.getAtt(type0Ref, "attScript0?json")
        )

        assertEquals(
            DataValue.create("[\"${type0Record.attForScript}\"]"),
            services.recordsService.getAtt(type0Ref, "attScript01[]")
        )

        assertEquals(
            DataValue.create(type0Record.attAttributeValue),
            services.recordsService.getAtt(type0Ref, "attScript1")
        )

        assertEquals(
            DataValue.create("{\"attAttributeValue\":\"${type0Record.attAttributeValue}\"}"),
            services.recordsService.getAtt(type0Ref, "attScript2?json")
        )
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
