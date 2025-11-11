package ru.citeck.ecos.records3.test.record.atts.value.factory.interfacelookup

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory

class InterfaceLookupFactoryTest {

    @Test
    fun testInterfaceBasedFactoryLookup() {
        val factory = RecordsServiceFactory()
        val records = factory.recordsService

        val dto = MyCustomTypeImpl()

        assertThat(records.getAtt(dto, "foo").asText()).isEqualTo("bar")
    }
}
