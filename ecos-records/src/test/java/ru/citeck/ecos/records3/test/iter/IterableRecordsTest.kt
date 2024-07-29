package ru.citeck.ecos.records3.test.iter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.iter.IterableRecordRefs
import ru.citeck.ecos.records3.iter.IterableRecords
import ru.citeck.ecos.records3.iter.IterableRecordsConfig
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class IterableRecordsTest {

    @Test
    fun equalityTest() {

        val query = RecordsQuery.create {
            withLanguage(PredicateService.LANGUAGE_PREDICATE)
            withSourceId("test")
        }
        val config = IterableRecordsConfig.create {
            withAttsToLoad(mapOf("aa" to "bb"))
            withPageSize(10)
        }
        val records = RecordsServiceFactory().recordsService

        val recs0 = IterableRecords(query.copy {}, config.copy {}, records)
        val recs1 = IterableRecords(query.copy {}, config.copy {}, records)

        assertThat(recs0.hashCode()).isEqualTo(recs1.hashCode())
        assertThat(recs0).isEqualTo(recs1)

        val recRefs0 = IterableRecordRefs(query.copy {}, config.copy {}, records)
        val recRefs1 = IterableRecordRefs(query.copy {}, config.copy {}, records)

        assertThat(recRefs0.hashCode()).isEqualTo(recRefs1.hashCode())
        assertThat(recRefs0).isEqualTo(recRefs1)
    }
}
