package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

class DtoSchemaTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        // var atts
        val atts = services.dtoSchemaReader.read(DtoSchema::class.java)

        assertEquals(4, atts.size)

        val attsMap = services.attSchemaWriter.writeToMap(atts)
        assertEquals("?localId", attsMap["id"])
        assertEquals("strField", attsMap["strField"])
        assertEquals("strFieldList[]", attsMap["strFieldList"])
        assertEquals("strFieldSet[]", attsMap["strFieldSet"])

        // val atts
        val atts2 = services.dtoSchemaReader.read(ValDtoSchema::class.java)

        assertEquals(8, atts2.size)

        val attsMap2 = services.attSchemaWriter.writeToMap(atts2)
        assertEquals("?localId", attsMap2["id"])
        assertEquals("strField", attsMap2["strField"])
        assertEquals("strFieldList[]", attsMap2["strFieldList"])
        assertEquals("strFieldSet[]", attsMap2["strFieldSet"])
        assertEquals("enumTest?str", attsMap2["enumTest"])
        assertEquals("enumListTest[]?str", attsMap2["enumListTest"])
        assertEquals("mapTest?json", attsMap2["mapTest"])
        assertEquals("hashMapTest?json", attsMap2["hashMapTest"])

        val valDtoValue = ValDtoSchema(
            "123",
            "456",
            listOf("abc", "def"),
            setOf("ghi", "jkl"),
            ValDtoSchema.EnumClass.SECOND,
            listOf(ValDtoSchema.EnumClass.FIRST, ValDtoSchema.EnumClass.SECOND),
            mapOf(Pair("one", "two")),
            hashMapOf(Pair("one", "two"))
        )
        val valDtoValue2 = services.recordsService.getAtts(valDtoValue, ValDtoSchema::class.java)

        assertEquals(valDtoValue, valDtoValue2)

        val attsMap3 = services.attSchemaWriter.writeToMap(services.dtoSchemaReader.read(DtoSchema2::class.java))
        assertEquals("?id|or('')", attsMap3["id"])

        val valDtoWithDefault = ValDtoWithDefault()
        val valDtoWithDefaultValue = services.recordsService.getAtts(valDtoWithDefault, ValDtoWithDefault::class.java)
        assertEquals(valDtoWithDefault, valDtoWithDefaultValue)

        val withDefaultSchema = services.dtoSchemaReader.read(ValDtoWithDefault::class.java)
        assertEquals(5, withDefaultSchema.size)
    }

    @Test
    fun test2() {

        val services = RecordsServiceFactory()

        val schema = services.dtoSchemaReader.read(TestDtoWithMetaAttAndSet::class.java)
        assertEquals(1, schema.size)
        assertEquals("abc", schema[0].name)
        assertEquals("attributes", schema[0].alias)
        assertTrue(schema[0].multiple)
        assertEquals("?disp", schema[0].inner[0].name)
    }

    class TestDtoWithMetaAttAndSet {
        @AttName("abc[]")
        var attributes: Set<String>? = emptySet()
    }

    data class ValDtoWithDefault(

        val roles: Set<String> = emptySet(),
        val permissions: Set<String> = emptySet(),

        val statuses: Set<String> = emptySet(),
        val condition: Predicate = VoidPredicate.INSTANCE,

        val type: ValDtoSchema.EnumClass = ValDtoSchema.EnumClass.FIRST
    )

    data class ValDtoSchema(
        val id: String?,
        val strField: String?,
        val strFieldList: List<String>?,
        val strFieldSet: Set<String>?,
        val enumTest: EnumClass?,
        val enumListTest: List<EnumClass>?,
        val mapTest: Map<String, Any>?,
        val hashMapTest: HashMap<String, Any>?
    ) {
        enum class EnumClass {
            FIRST,
            SECOND
        }
    }

    class DtoSchema(
        var id: String?,
        var strField: String?,
        var strFieldList: List<String>?,
        var strFieldSet: Set<String>?
    )

    class DtoSchema2(
        var id: EntityRef
    )
}
