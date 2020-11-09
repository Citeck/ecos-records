package ru.citeck.ecos.records2.test.local

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.QueryContext
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import kotlin.test.assertEquals

class LocalInitWithMetaFieldTest : LocalRecordsDao(), LocalRecordsMetaDao<Any> {

    private var innerAttsMapList: MutableList<HashMap<String, String>> = mutableListOf()
    private val factory = RecordsServiceFactory()

    private val attsToReq = linkedMapOf(
        Pair("first", "second"),
        Pair(".att(n:\"first2\"){str}", ".att(n:\"second2\"){str}")
    )

    override fun getId() = "test"

    @Test
    fun test() {

        factory.recordsService.register(this)
        factory.recordsService.getAttributes(RecordRef.create("test", "test"), attsToReq)

        val expectedAtts = hashMapOf(
            Pair("second", ".att(n:\"second\"){disp:disp}"),
            Pair("second2", ".att(n:\"second2\"){str:str}")
        )

        assertEquals(mutableListOf(expectedAtts, expectedAtts), innerAttsMapList)
    }

    override fun getLocalRecordsMeta(records: List<RecordRef>, metaField: MetaField): List<Any> {
        return records.map { Value(it) }
    }

    inner class Value(val id: RecordRef) : MetaValue {

        override fun <T : QueryContext?> init(context: T, field: MetaField) {
            if (id.id == "test") {
                innerAttsMapList.add(HashMap(field.innerAttributesMap))
                factory.recordsService.getAtt(RecordRef.create("test", "test2"), "?id")
                innerAttsMapList.add(HashMap(field.innerAttributesMap))
            }
        }
    }
}
