package ru.citeck.ecos.records2.test.local

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.promise.Promise
import kotlin.test.assertEquals

class LocalInitWithMetaFieldTest : RecordAttsDao {

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
        factory.recordsService.getAtts(EntityRef.create("test", "test"), attsToReq)

        val expectedAtts = hashMapOf(
            Pair("second", "second"),
            Pair("second2", "second2?str")
        )

        assertEquals(mutableListOf(expectedAtts, expectedAtts), innerAttsMapList)
    }

    override fun getRecordAtts(recordId: String): Any {
        return Value(EntityRef.create(getId(), recordId))
    }

    inner class Value(val id: EntityRef) : AttValue {

        override fun init(): Promise<*>? {
            if (id.getLocalId() == "test") {
                innerAttsMapList.add(HashMap(AttContext.getInnerAttsMap()))
                factory.recordsService.getAtt(EntityRef.create("test", "test2"), "?id")
                innerAttsMapList.add(HashMap(AttContext.getInnerAttsMap()))
            }
            return null
        }
    }
}
