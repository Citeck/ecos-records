package ru.citeck.ecos.records2.test.local

import ecos.com.fasterxml.jackson210.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ListValueTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        val srcArrayOfStr = listOf("first", "second", "third")
        val srcArrayOfObj = listOf(
            ObjectData.create("""{"key1":"value1"}"""),
            ObjectData.create("""{"key2":"value2"}""")
        )

        val recordsById = mapOf(
            "arrayOfStr" to SomeDto(
                Json.mapper.convert(srcArrayOfStr, com.fasterxml.jackson.databind.JsonNode::class.java)!!,
                Json.mapper.convert(srcArrayOfStr, JsonNode::class.java)!!,
                Json.mapper.convert(srcArrayOfStr, DataValue::class.java)!!
            ),
            "arrayOfObj" to SomeDto(
                Json.mapper.convert(srcArrayOfObj, com.fasterxml.jackson.databind.JsonNode::class.java)!!,
                Json.mapper.convert(srcArrayOfObj, JsonNode::class.java)!!,
                Json.mapper.convert(srcArrayOfObj, DataValue::class.java)!!
            )
        )
        services.recordsService.register(object : LocalRecordsDao(), LocalRecordsMetaDao<Any> {
            override fun getLocalRecordsMeta(records: MutableList<EntityRef>, metaField: MetaField): List<Any> {
                return records.map { recordsById[it.getLocalId()] ?: EmptyAttValue.INSTANCE }
            }
            override fun getId(): String {
                return "dao"
            }
        })

        val getListAtt: (id: String, att: String) -> List<Any> = { id, att ->
            val ref = EntityRef.valueOf("dao@$id")
            services.recordsService.getAtt(ref, att).asList(Any::class.java)
        }

        val assertAtts: (id: String, att: String, expectedList: List<Any>) -> Unit = { id, att, obj ->
            val listRes = getListAtt(id, att)
            assertThat(Json.mapper.convert<Any>(listRes, Json.mapper.getListType(obj[0]::class.java))!!).isEqualTo(obj)
        }

        assertAtts("arrayOfStr", "jsonNodeArr[]?str", srcArrayOfStr)
        assertAtts("arrayOfStr", "legacyJsonNodeArr[]?str", srcArrayOfStr)
        assertAtts("arrayOfStr", "dataValue[]?str", srcArrayOfStr)

        assertAtts("arrayOfObj", "jsonNodeArr[]?json", srcArrayOfObj)
        assertAtts("arrayOfObj", "legacyJsonNodeArr[]?json", srcArrayOfObj)
        assertAtts("arrayOfObj", "dataValue[]?json", srcArrayOfObj)
    }

    class SomeDto(
        val legacyJsonNodeArr: com.fasterxml.jackson.databind.JsonNode,
        val jsonNodeArr: JsonNode,
        val dataValue: DataValue
    )
}
