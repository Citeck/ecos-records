package ru.citeck.ecos.records3.test.record.atts.schema.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

class SchemaResolverLocalIdTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsService

        val recordsDao = RecordsDaoBuilder.create("test")
            .addRecord("empty-source-id", TestClassWithEmptySourceId())
            .addRecord("with-source-id", TestClassWithSourceId())
            .addRecord("empty-source-id-meta", TestClassWithEmptySourceIdMetaValue())
            .addRecord("with-source-id-meta", TestClassWithSourceIdMetaValue())
            .build() as InMemRecordsDao<*>

        records.register(recordsDao)

        recordsDao.records.forEach {
            val localId = records.getAtt(EntityRef.create("test", it.key), "?localId").asText()
            assertEquals(it.key, localId)
        }

        val queryRes = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withQuery(Predicates.contains("?localId", "source"))
                .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                .build(),
            listOf("?localId")
        ).getRecords().map { it.getAtt("?localId").asText() }

        assertThat(queryRes).containsExactlyInAnyOrder(*recordsDao.records.map { it.key }.toTypedArray())

        recordsDao.records.forEach { keyRec ->
            val localId = records.getAtt(keyRec.value, "?localId").asText()
            val idFromValue = if (keyRec.value is AttValue) {
                (keyRec.value as AttValue).id as String
            } else {
                (keyRec.value as AttValue).id
            }
            assertThat(localId).isEqualTo(EntityRef.valueOf(idFromValue).getLocalId())
        }
    }

    class TestClassWithEmptySourceId : AttValue {
        override fun getId(): Any {
            return "appName/@empty-source-id"
        }
    }

    class TestClassWithSourceId : AttValue {
        override fun getId(): Any {
            return "appName/sourceId@with-source-id"
        }
    }

    class TestClassWithEmptySourceIdMetaValue : AttValue {
        override fun getId(): String {
            return "appName/@empty-source-id-meta"
        }
    }

    class TestClassWithSourceIdMetaValue : AttValue {
        override fun getId(): String {
            return "appName/sourceId@with-source-id-meta"
        }
    }
}
