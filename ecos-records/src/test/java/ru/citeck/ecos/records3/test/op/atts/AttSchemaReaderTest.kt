package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import kotlin.test.assertEquals

class AttSchemaReaderTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        val reader = factory.attSchemaReader

        val atts = reader.read("aa{bb:cc,dd:ee,}")
        assertEquals(2, atts.inner.size)
        assertEquals("bb", atts.inner[0].alias)
        assertEquals("cc", atts.inner[0].name)
        assertEquals("dd", atts.inner[1].alias)
        assertEquals("ee", atts.inner[1].name)
    }
}
