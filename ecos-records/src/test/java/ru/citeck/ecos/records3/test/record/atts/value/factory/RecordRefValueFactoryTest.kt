package ru.citeck.ecos.records3.test.record.atts.value.factory

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValue
import kotlin.test.assertEquals

class RecordRefValueFactoryTest {

    @Test
    fun test() {

        val test1Ref = RecordRef.valueOf("test@test1")
        val testRecord1 = TestRecord1(test1Ref)

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("test", TestRecord0(test1Ref))
                .addRecord("test1", testRecord1)
                .build()
        )
        val testRef = RecordRef.valueOf("test@test")

        assertEquals(testRecord1.type.toString(), records.getAtt(testRef, "ref._type?id").asText())
        assertEquals(testRecord1.displayName.toString(), records.getAtt(testRef, "ref?disp").asText())
        assertEquals(test1Ref.toString(), records.getAtt(testRef, "ref?str").asText())
        assertEquals(testRecord1.asDouble(), records.getAtt(testRef, "ref?num").asDouble())
        assertEquals(testRecord1.asBoolean(), records.getAtt(testRef, "ref?bool").asBoolean())
        assertEquals(DataValue.create(testRecord1.asJson()), records.getAtt(testRef, "ref?json"))

        assertEquals(records.getAtt(testRef, "ref.ref?id"), records.getAtt(testRef, "ref.ref?assoc"))
        assertEquals(records.getAtt(testRef, "ref.ref.ref?id"), records.getAtt(testRef, "ref.ref.ref?assoc"))

        val idValue = records.getAtt(testRef, "ref?id")

        val res = records.getAtts(
            testRef,
            mapOf(
                "first" to "ref.ref:withDots.ref:withDots?id",
                "second" to "ref.ref:withDots.ref:withDots?id"
            )
        )
        val expected = ObjectData.create()
        expected.set("first", idValue)
        expected.set("second", idValue)

        assertEquals(expected, res.getAtts())
    }

    class TestRecord0(
        val ref: RecordRef
    )

    class TestRecord1(val ref: RecordRef) : AttValue {

        override fun getId(): Any? {
            return "abc"
        }

        override fun getDisplayName(): Any? {
            return "dispname"
        }

        override fun asDouble(): Double? {
            return 111.0
        }

        override fun asBoolean(): Boolean? {
            return true
        }

        override fun asJson(): Any? {
            return ObjectData.create("{\"aa\":\"bb\"}")
        }

        override fun getType(): RecordRef {
            return RecordRef.valueOf("abc@def")
        }

        override fun getAtt(name: String?): Any? {
            if (name == "ref") {
                return ref
            }
            if (name == "ref:withDots") {
                return ref
            }
            return null
        }
    }
}
