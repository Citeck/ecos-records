package ru.citeck.ecos.records2.test.local

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.QueryContext
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef
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
        factory.recordsService.getAttributes(EntityRef.create("test", "test"), attsToReq)

        val expectedAtts = hashMapOf(
            Pair("second", "second"),
            Pair("second2", "second2?str")
        )

        assertEquals(mutableListOf(expectedAtts, expectedAtts), innerAttsMapList)
    }

    override fun getLocalRecordsMeta(records: List<EntityRef>, metaField: MetaField): List<Any> {
        return records.map { Value(it) }
    }

    inner class Value(val id: EntityRef) : MetaValue {

        override fun <T : QueryContext?> init(context: T, field: MetaField) {
            if (id.getLocalId() == "test") {
                innerAttsMapList.add(HashMap(field.innerAttributesMap))
                factory.recordsService.getAtt(EntityRef.create("test", "test2"), "?id")
                innerAttsMapList.add(HashMap(field.innerAttributesMap))
            }
        }
    }
}
