package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsServiceFactory
import kotlin.test.assertEquals

class DtoSchemaTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        // var atts
        val atts = services.dtoSchemaReader.read(DtoSchema::class.java)

        assertEquals(4, atts.size)

        val attsMap = services.attSchemaWriter.writeToMap(atts)
        assertEquals(".localId", attsMap["id"])
        assertEquals(".att(n:\"strField\"){disp}", attsMap["strField"])
        assertEquals(".atts(n:\"strFieldList\"){disp}", attsMap["strFieldList"])
        assertEquals(".atts(n:\"strFieldSet\"){disp}", attsMap["strFieldSet"])

        // val atts
        val atts2 = services.dtoSchemaReader.read(ValDtoSchema::class.java)

        assertEquals(8, atts2.size)

        val attsMap2 = services.attSchemaWriter.writeToMap(atts2)
        assertEquals(".localId", attsMap2["id"])
        assertEquals(".att(n:\"strField\"){disp}", attsMap2["strField"])
        assertEquals(".atts(n:\"strFieldList\"){disp}", attsMap2["strFieldList"])
        assertEquals(".atts(n:\"strFieldSet\"){disp}", attsMap2["strFieldSet"])
        assertEquals(".att(n:\"enumTest\"){str}", attsMap2["enumTest"])
        assertEquals(".atts(n:\"enumListTest\"){str}", attsMap2["enumListTest"])
        assertEquals(".att(n:\"mapTest\"){json}", attsMap2["mapTest"])
        assertEquals(".att(n:\"hashMapTest\"){json}", attsMap2["hashMapTest"])

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
        val valDtoValue2 = services.recordsServiceV1.getAtts(valDtoValue, ValDtoSchema::class.java)

        assertEquals(valDtoValue, valDtoValue2)

        val attsMap3 = services.attSchemaWriter.writeToMap(services.dtoSchemaReader.read(DtoSchema2::class.java))
        assertEquals(".id", attsMap3["id"])

        val valDtoWithDefault = ValDtoWithDefault()
        val valDtoWithDefaultValue = services.recordsServiceV1.getAtts(valDtoWithDefault, ValDtoWithDefault::class.java)
        assertEquals(valDtoWithDefault, valDtoWithDefaultValue)

        val withDefaultSchema = services.dtoSchemaReader.read(ValDtoWithDefault::class.java)
        assertEquals(5, withDefaultSchema.size)
    }

    data class ValDtoWithDefault(

        val roles: Set<String> = emptySet(),
        val permissions: Set<String> = emptySet(),

        val statuses: Set<String> = emptySet(),
        val condition: Predicate = VoidPredicate.INSTANCE,

        val type: ValDtoSchema.EnumClass = ValDtoSchema.EnumClass.FIRST
    )

    data class ValDtoSchema(
        val id: String,
        val strField: String,
        val strFieldList: List<String>,
        val strFieldSet: Set<String>,
        val enumTest: EnumClass,
        val enumListTest: List<EnumClass>,
        val mapTest: Map<String, Any>,
        val hashMapTest: HashMap<String, Any>
    ) {
        enum class EnumClass {
            FIRST, SECOND
        }
    }

    class DtoSchema(
        var id: String,
        var strField: String,
        var strFieldList: List<String>,
        var strFieldSet: Set<String>
    )

    class DtoSchema2(
        var id: RecordRef
    )
}
