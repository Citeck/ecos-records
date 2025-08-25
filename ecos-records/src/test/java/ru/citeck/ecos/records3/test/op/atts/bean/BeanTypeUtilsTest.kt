package ru.citeck.ecos.records3.test.op.atts.bean

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.factory.bean.BeanTypeUtils

class BeanTypeUtilsTest {

    @Test
    fun test() {

        val ctx = BeanTypeUtils.getTypeContext(DataClass::class.java)
        assertTrue(ctx.hasProperty("_field"))
        assertTrue(ctx.hasProperty("boolProp"))
        // for legacy support
        assertTrue(ctx.hasProperty(RecordConstants.ATT_TYPE))

        val ctx2 = BeanTypeUtils.getTypeContext(ClassWithExtension::class.java)
        val ctx2value = ObjectData.create(
            """{
            "inner2": {
                "field0": "abc",
                "field1": 456
            },
            "field0": "def",
            "field1": 123
        }
            """.trimIndent()
        )

        val dto = ClassWithExtension(null, null)
        ctx2.applyData(dto, ctx2value)

        assertEquals("abc", dto.inner2!!.field0)
        assertEquals(456, dto.inner2!!.field1)
        assertEquals("def", dto.inner!!.field0)
        assertEquals(123, dto.inner!!.field1)
    }

    data class DataClass(
        @AttName("_field")
        val strWithAnn: String,
        @AttName("?type")
        val typeField: String,
        val boolProp: Boolean
    )

    data class ClassWithExtension(
        var inner2: ExtensionInner?,
        @AttName("...")
        var inner: ExtensionInner?
    )

    data class ExtensionInner(
        var field0: String,
        var field1: Int
    )
}
