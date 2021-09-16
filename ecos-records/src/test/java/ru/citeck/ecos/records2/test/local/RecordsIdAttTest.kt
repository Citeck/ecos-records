package ru.citeck.ecos.records2.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.request.query.RecordsQueryResult
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class RecordsIdAttTest {

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun createProperties(): RecordsProperties {
                val props = super.createProperties()
                props.appName = "app0"
                props.appInstanceId = "app0-123"
                return props
            }
        }
        val props = services.properties
        val records = services.recordsServiceV1

        val results = listOf("first", "second")

        // withoutAddSourceIdWithoutMetaSourceId

        val withoutAddSourceIdWithoutMetaSourceId = "withoutAddSourceIdWithoutMeta"
        val daoWithoutAddSourceIdWithoutMeta = object : LocalRecordsDao(false), LocalRecordsQueryDao {
            init { id = withoutAddSourceIdWithoutMetaSourceId }
            override fun queryLocalRecords(query: ru.citeck.ecos.records2.request.query.RecordsQuery): RecordsQueryResult<RecordRef> {
                return RecordsQueryResult.of(*(results.map { RecordRef.valueOf(it) }).toTypedArray())
            }
        }
        services.recordsService.register(daoWithoutAddSourceIdWithoutMeta)

        val withoutAddSourceIdWithoutMetaSourceIdResult = records.query(
            RecordsQuery.create {
                withSourceId(withoutAddSourceIdWithoutMetaSourceId)
            }
        )
        assertThat(withoutAddSourceIdWithoutMetaSourceIdResult.getRecords()).containsExactlyElementsOf(
            results.map {
                RecordRef.create(props.appName, withoutAddSourceIdWithoutMetaSourceId, it)
            }
        )

        // withAddSourceIdWithoutMetaSourceId

        val withAddSourceIdWithoutMetaSourceId = "withAddSourceIdWithoutMetaSourceId"
        val daoWithAddSourceIdWithoutMetaSourceId = object : LocalRecordsDao(true), LocalRecordsQueryDao {
            init { id = withAddSourceIdWithoutMetaSourceId }
            override fun queryLocalRecords(query: ru.citeck.ecos.records2.request.query.RecordsQuery): RecordsQueryResult<RecordRef> {
                return RecordsQueryResult.of(*(results.map { RecordRef.valueOf(it) }).toTypedArray())
            }
        }
        services.recordsService.register(daoWithAddSourceIdWithoutMetaSourceId)

        val withAddSourceIdWithoutMetaSourceIdResult = records.query(
            RecordsQuery.create {
                withSourceId(withAddSourceIdWithoutMetaSourceId)
            }
        )
        assertThat(withAddSourceIdWithoutMetaSourceIdResult.getRecords()).containsExactlyElementsOf(
            results.map {
                RecordRef.create(props.appName, withAddSourceIdWithoutMetaSourceId, it)
            }
        )

        // withoutAddSourceIdWithMetaSourceId

        val withoutAddSourceIdWithMetaSourceId = "withoutAddSourceIdWithMetaSourceId"
        val daoWithoutAddSourceIdWithMetaSourceId = object : LocalRecordsDao(false), LocalRecordsQueryWithMetaDao<TestDao> {
            init { id = withoutAddSourceIdWithMetaSourceId }

            override fun queryLocalRecords(
                recordsQuery: ru.citeck.ecos.records2.request.query.RecordsQuery,
                field: MetaField
            ): RecordsQueryResult<TestDao> {
                return RecordsQueryResult.of(*(results.map { TestDao(it) }).toTypedArray())
            }
        }
        services.recordsService.register(daoWithoutAddSourceIdWithMetaSourceId)

        val withoutAddSourceIdWithMetaSourceIdResult = records.query(
            RecordsQuery.create {
                withSourceId(withoutAddSourceIdWithMetaSourceId)
            },
            mapOf(
                "idAtt" to "?id"
            )
        )
        assertThat(withoutAddSourceIdWithMetaSourceIdResult.getRecords().map { it.getId() }).containsExactlyElementsOf(
            results.map {
                RecordRef.create(props.appName, withoutAddSourceIdWithMetaSourceId, it)
            }
        )
        assertThat(withoutAddSourceIdWithMetaSourceIdResult.getRecords().map { it.getAtts().get("idAtt").asText() }).containsExactlyElementsOf(
            results.map {
                RecordRef.create(props.appName, withoutAddSourceIdWithMetaSourceId, it).toString()
            }
        )

        // withAddSourceIdWithMetaSourceId

        val withAddSourceIdWithMetaSourceId = "withAddSourceIdWithMetaSourceId"
        val daoWithAddSourceIdWithMetaSourceId = object : LocalRecordsDao(true), LocalRecordsQueryWithMetaDao<TestDao> {
            init { id = withAddSourceIdWithMetaSourceId }

            override fun queryLocalRecords(
                recordsQuery: ru.citeck.ecos.records2.request.query.RecordsQuery,
                field: MetaField
            ): RecordsQueryResult<TestDao> {
                return RecordsQueryResult.of(*(results.map { TestDao(it) }).toTypedArray())
            }
        }
        services.recordsService.register(daoWithAddSourceIdWithMetaSourceId)

        val withAddSourceIdWithMetaSourceIdResult = records.query(
            RecordsQuery.create {
                withSourceId(withAddSourceIdWithMetaSourceId)
            },
            mapOf(
                "idAtt" to "?id"
            )
        )
        assertThat(withAddSourceIdWithMetaSourceIdResult.getRecords().map { it.getId() }).containsExactlyElementsOf(
            results.map {
                RecordRef.create(props.appName, withAddSourceIdWithMetaSourceId, it)
            }
        )
        assertThat(withAddSourceIdWithMetaSourceIdResult.getRecords().map { it.getAtts().get("idAtt").asText() }).containsExactlyElementsOf(
            results.map {
                RecordRef.create(props.appName, withAddSourceIdWithMetaSourceId, it).toString()
            }
        )
    }

    class TestDao(val id: String)
}
