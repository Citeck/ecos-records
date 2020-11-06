package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import kotlin.test.assertEquals

class DtoSchemaTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val atts = services.dtoSchemaReader.read(DtoSchema::class.java)

        assertEquals(4, atts.size)

        val attsMap = services.attSchemaWriter.writeToMap(atts)
        assertEquals(".localId", attsMap["id"])
        assertEquals(".att(n:\"strField\"){disp}", attsMap["strField"])
        assertEquals(".atts(n:\"strFieldList\"){disp}", attsMap["strFieldList"])
        assertEquals(".atts(n:\"strFieldSet\"){disp}", attsMap["strFieldSet"])
    }

    class DtoSchema(
        var id: String,
        var strField: String,
        var strFieldList: List<String>,
        var strFieldSet: Set<String>
    )
}
