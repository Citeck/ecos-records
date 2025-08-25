package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory

class AutoAliasTest {

    @Test
    fun test() {

        val factory = RecordsServiceFactory()
        val result = factory.recordsService.getAtt(
            DataValue.create("{\"inner\":{\"key\":\"value\",\"key2\":\"value2\"}}"),
            "inner{key?str,aa:key?disp}"
        )

        assertTrue(result.has("key"))
        assertTrue(result.has("aa"))
    }
}
