package ru.citeck.ecos.records3.test.record.dao.impl.ext

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDao
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDaoConfig
import ru.citeck.ecos.records3.record.dao.impl.ext.impl.ReadOnlyMapExtStorage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class ExtStorageRecordsDaoTest {

    @Test
    fun test() {

        val map = mapOf(
            "abc" to "def",
            "abcdef" to "hij"
        )

        val records = RecordsServiceFactory().recordsService
        val daoConfig = ExtStorageRecordsDaoConfig.create(ReadOnlyMapExtStorage(map))
            .withEcosType("customType")
            .withSourceId("test")
            .build()
        records.register(ExtStorageRecordsDao(daoConfig))

        assertThat(records.getAtt("test@abc", "?str").asText()).isEqualTo("def")
        assertThat(records.getAtt("test@abcdef", "?str").asText()).isEqualTo("hij")

        val queryRes = RecordsQuery.create()
            .withQuery(
                Predicates.and(
                    Predicates.contains("?str", "def"),
                    Predicates.eq("_type", "emodel/type@customType")
                )
            )
            .withSourceId("test")
            .build()

        val queryResult = records.query(queryRes)
        assertThat(queryResult.getRecords()).hasSize(1)
        assertThat(queryResult.getRecords()[0].toString()).isEqualTo("test@def")
    }
}
