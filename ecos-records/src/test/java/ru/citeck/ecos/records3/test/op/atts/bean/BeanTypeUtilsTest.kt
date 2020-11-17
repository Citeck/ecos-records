package ru.citeck.ecos.records3.test.op.atts.bean

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.op.atts.service.value.factory.bean.BeanTypeUtils
import kotlin.test.assertTrue

class BeanTypeUtilsTest {

    @Test
    fun test() {
        val ctx = BeanTypeUtils.getTypeContext(DataClass::class.java)
        assertTrue(ctx.hasProperty("_field"))
        assertTrue(ctx.hasProperty("boolProp"))
        // for legacy support
        assertTrue(ctx.hasProperty(RecordConstants.ATT_TYPE))
    }

    data class DataClass(
        @AttName("_field")
        val strWithAnn: String,
        @AttName("?type")
        val typeField: String,
        val boolProp: Boolean
    )
}
