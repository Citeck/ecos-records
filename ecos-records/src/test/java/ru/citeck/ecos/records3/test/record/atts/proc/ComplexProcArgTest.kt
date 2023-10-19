package ru.citeck.ecos.records3.test.record.atts.proc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ComplexProcArgTest {

    @Test
    fun orElseTest() {

        val records = RecordsServiceFactory().recordsServiceV1

        var res: DataValue = records.getAtt(EntityRef.create("meta", ""), "unknown.unknown[]![]")
        assertThat(res).isEqualTo(DataValue.createArr())

        res = records.getAtt(EntityRef.create("meta", ""), "unknown.unknown{a:b}!{}")
        assertThat(res).isEqualTo(DataValue.createObj())

        res = records.getAtt(EntityRef.create("meta", ""), "unknown.unknown[]![]|join('==')")
        assertThat(res).isEqualTo(DataValue.createStr(""))

        res = records.getAtt(EntityRef.create("meta", ""), "unknown.unknown[]![\"aa\",\"bb\"]|join('==')")
        assertThat(res).isEqualTo(DataValue.createStr("aa==bb"))
    }

    @Test
    fun readerTest() {

        val procReader = RecordsServiceFactory().attProcReader
        val readerRes = procReader.read("att|presuf('[',']')")

        assertThat(readerRes.processors).containsExactly(
            AttProcDef(
                "presuf",
                listOf(
                    DataValue.createStr("["),
                    DataValue.createStr("]")
                )
            )
        )
    }
}
