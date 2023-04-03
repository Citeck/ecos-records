package ru.citeck.ecos.records3.test.record.atts.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.Version
import ru.citeck.ecos.records3.RecordsServiceFactory

class VersionValueTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1
        val version = Version.valueOf("1.0.0")

        val strValue = records.getAtt(version, "?str").asText()
        assertThat(strValue).isEqualTo("1.0.0")
    }
}
