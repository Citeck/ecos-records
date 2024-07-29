package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import kotlin.test.assertEquals

class DtoAttsInstantiateTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()

        val data = AttsClass("123", "456", 456)
        val dataRes = services.recordsService.getAtts(data, AttsClass::class.java)

        assertEquals(data, dataRes)

        val dataMut = AttsMutClass("123", "456", 456)
        val dataMutRes = services.recordsService.getAtts(dataMut, AttsMutClass::class.java)

        assertEquals(dataMut, dataMutRes)
    }

    data class AttsClass(
        @AttName("abcd")
        val first: String,
        val second: String,
        val third: Int
    )

    data class AttsMutClass(
        @AttName("abcd")
        var first: String,
        var second: String,
        var third: Int
    )
}
