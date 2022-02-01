package ru.citeck.ecos.records3.test.op.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao

class CreateWithAssocsTest {

    @ParameterizedTest
    @ValueSource(strings = ["?assoc", "?str", ""])
    fun test(assocScalar: String) {

        val rec1Alias = "source-id@-alias-1"

        val rec0 = RecordAtts(RecordRef.create("source-id", ""))
        rec0.setAtt("assocArr$assocScalar", listOf(rec1Alias))
        rec0.setAtt("assocStr$assocScalar", rec1Alias)

        val rec1 = RecordAtts(RecordRef.create("source-id", ""))
        rec1.setAtt("_alias", "source-id@-alias-1")

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao("source-id"))

        val mutRes = records.mutate(listOf(rec0, rec1))

        val assocStrRes = records.getAtt(mutRes[0], "assocStr?id").asText()
        assertThat(assocStrRes).isEqualTo(mutRes[1].toString())
        val assocArrRes = records.getAtt(mutRes[0], "assocArr[]?id").asStrList()[0]
        assertThat(assocArrRes).isEqualTo(mutRes[1].toString())
    }
}
