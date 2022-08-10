package ru.citeck.ecos.records2.test.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.exception.LanguageNotSupportedException
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.SupportsQueryLanguages
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class LanguageNotSupportedExceptionTest {

    @Test
    fun test() {

        val dao = object : RecordsQueryDao, SupportsQueryLanguages {
            override fun getId(): String = "test"
            override fun queryRecords(recsQuery: RecordsQuery): Any? = null
            override fun getSupportedLanguages(): List<String> = listOf("predicate")
        }
        val records = RecordsServiceFactory().recordsServiceV1
        records.register(dao)

        assertThrows<LanguageNotSupportedException> {
            records.query(
                RecordsQuery.create {
                    withSourceId("test")
                    withLanguage("unknown")
                }
            )
        }
    }
}
