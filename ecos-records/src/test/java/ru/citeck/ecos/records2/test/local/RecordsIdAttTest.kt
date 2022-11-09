package ru.citeck.ecos.records2.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.test.EcosWebAppContextMock
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.request.query.RecordsQueryResult
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext

class recordIdsAttTest {

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppContext(): EcosWebAppContext {
                return EcosWebAppContextMock("app0")
            }
        }
        val props = services.webappProps
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
        val daoWithoutAddSourceIdWithMetaSourceId = object : LocalRecordsDao(false), LocalRecordsQueryWithMetaDao<TestDto> {
            init { id = withoutAddSourceIdWithMetaSourceId }

            override fun queryLocalRecords(
                recordsQuery: ru.citeck.ecos.records2.request.query.RecordsQuery,
                field: MetaField
            ): RecordsQueryResult<TestDto> {
                return RecordsQueryResult.of(*(results.map { TestDto(it) }).toTypedArray())
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
        val daoWithAddSourceIdWithMetaSourceId = object : LocalRecordsDao(true), LocalRecordsQueryWithMetaDao<TestDto> {
            init { id = withAddSourceIdWithMetaSourceId }

            override fun queryLocalRecords(
                recordsQuery: ru.citeck.ecos.records2.request.query.RecordsQuery,
                field: MetaField
            ): RecordsQueryResult<TestDto> {
                return RecordsQueryResult.of(*(results.map { TestDto(it) }).toTypedArray())
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

        // getAtt without add source id

        val getAttWithoutAddSourceId = "getAttWithoutAddSourceId"
        val getAttWithoutAddSourceIdDao = object : LocalRecordsDao(false), LocalRecordsMetaDao<TestDto> {
            init { id = getAttWithoutAddSourceId }

            override fun getLocalRecordsMeta(
                records: MutableList<RecordRef>,
                metaField: MetaField
            ): MutableList<TestDto> {
                return records.map { TestDto(it.id) }.toMutableList()
            }
        }
        services.recordsService.register(getAttWithoutAddSourceIdDao)

        val getAttWithoutAddSourceIdRes = records.getAtt(RecordRef.create(getAttWithoutAddSourceId, "localId"), "?id")
        assertThat(getAttWithoutAddSourceIdRes.asText()).isEqualTo(
            RecordRef.create(props.appName, getAttWithoutAddSourceId, "localId").toString()
        )

        // getAtt with add source id

        val getAttWithAddSourceId = "getAttWithAddSourceId"
        val getAttWithAddSourceIdDao = object : LocalRecordsDao(true), LocalRecordsMetaDao<TestDto> {
            init { id = getAttWithAddSourceId }

            override fun getLocalRecordsMeta(
                records: MutableList<RecordRef>,
                metaField: MetaField
            ): MutableList<TestDto> {
                return records.map { TestDto(it.id) }.toMutableList()
            }
        }
        services.recordsService.register(getAttWithAddSourceIdDao)

        val getAttWithAddSourceIdRes = records.getAtt(RecordRef.create(getAttWithAddSourceId, "localId"), "?id")
        assertThat(getAttWithAddSourceIdRes.asText()).isEqualTo(
            RecordRef.create(props.appName, getAttWithAddSourceId, "localId").toString()
        )
    }

    class TestDto(val id: String)
}
