package ru.citeck.ecos.records3.test.op.atts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

class StringAuthorityConvertersTest {

    @Test
    fun test() {

        val factory = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock()
            }
        }
        val conv = factory.attValuesConverter

        assertEquals(
            EntityRef.valueOf("emodel/person@admin"),
            conv.toAttValue("admin")!!.getAs("personRef")
        )
        assertEquals(
            EntityRef.valueOf("emodel/authority-group@MANAGERS"),
            conv.toAttValue("MANAGERS")!!.getAs("authorityGroupRef")
        )
        assertEquals(
            EntityRef.valueOf("emodel/person@admin"),
            conv.toAttValue("admin")!!.getAs("authorityRef")
        )
        assertEquals(
            EntityRef.valueOf("emodel/authority-group@MANAGERS"),
            conv.toAttValue("GROUP_MANAGERS")!!.getAs("authorityRef")
        )
    }
}
