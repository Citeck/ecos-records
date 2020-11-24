package ru.citeck.ecos.records3.test.op.atts.computed

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.computed.ComputedAtt
import ru.citeck.ecos.records3.record.op.atts.service.computed.ComputedAttType
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.type.RecordTypeService
import kotlin.test.assertEquals

class ComputedQueryTest {

    companion object {
        val type0 = RecordRef.valueOf("type0")
    }

    @Test
    fun test() {

        val type0Atts = listOf(
            ComputedAtt.create()
                .withId("attScript0")
                .withType(ComputedAttType.SCRIPT)
                .withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair(
                                "script",
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
                ).build(),
            ComputedAtt.create()
                .withId("attScript1")
                .withType(ComputedAttType.SCRIPT)
                .withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair(
                                "script",
                                """
                        return Records.get('test@type0Record').load('attAttributeValue');
                                """.trimIndent()
                            )
                        )
                    )
                ).build(),
            ComputedAtt.create()
                .withId("attScript2")
                .withType(ComputedAttType.SCRIPT)
                .withConfig(
                    ObjectData.create(
                        mapOf(
                            Pair(
                                "script",
                                """
                        return Records.get('test@type0Record').load(['attAttributeValue']);
                                """.trimIndent()
                            )
                        )
                    )
                ).build()
        )

        val services = object : RecordsServiceFactory() {
            override fun createRecordTypeService(): RecordTypeService {
                return object : RecordTypeService {
                    override fun getComputedAtts(typeRef: RecordRef): List<ComputedAtt> {
                        return if (typeRef == type0) {
                            type0Atts
                        } else {
                            emptyList()
                        }
                    }
                }
            }
        }

        val type0Record = RecordValue("type0Record", type0, "first", "second")
        val type0Ref = RecordRef.create("test", type0Record.id)

        services.recordsServiceV1.register(
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
            services.recordsServiceV1.getAtt(type0Ref, "attScript0?json")
        )

        assertEquals(
            DataValue.create(type0Record.attAttributeValue),
            services.recordsServiceV1.getAtt(type0Ref, "attScript1")
        )

        assertEquals(
            DataValue.create("{\"attAttributeValue\":\"${type0Record.attAttributeValue}\"}"),
            services.recordsServiceV1.getAtt(type0Ref, "attScript2?json")
        )
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
