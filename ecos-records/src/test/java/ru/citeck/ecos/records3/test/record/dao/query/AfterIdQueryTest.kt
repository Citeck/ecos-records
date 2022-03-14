package ru.citeck.ecos.records3.test.record.dao.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.request.query.RecordsQueryResult
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class AfterIdQueryTest {

    @Test
    fun test() {

        val daoV1 = object : RecordsQueryDao {
            override fun getId() = "test-v1"
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                return recsQuery
            }
        }
        val daoV0 = object : LocalRecordsDao(), LocalRecordsQueryWithMetaDao<Any> {
            override fun queryLocalRecords(
                recordsQuery: ru.citeck.ecos.records2.request.query.RecordsQuery,
                field: MetaField
            ): RecordsQueryResult<Any> {
                val result = RecordsQueryResult<Any>()
                result.addRecord(recordsQuery)
                return result
            }
            override fun getId(): String {
                return "test-v0"
            }
        }
        val factory = RecordsServiceFactory()
        factory.recordsService.register(daoV0)

        val records = factory.recordsServiceV1
        records.register(daoV1)

        val checkQuery = { query: RecordsQuery, expectedAfterId: Boolean ->
            assertThat(records.queryOne(query, "afterIdMode").asText()).withFailMessage {
                "SourceId: ${query.sourceId} page: ${query.page}"
            }.isEqualTo(expectedAfterId.toString())
        }

        val checkAfterIdMode = { sourceId: String, afterId: RecordRef? ->
            checkQuery(
                RecordsQuery.create {
                    withSourceId(sourceId)
                    withPage(
                        QueryPage.create {
                            withAfterId(afterId)
                        }
                    )
                },
                true
            )
        }

        listOf("test-v1", "test-v0").forEach { sourceId ->
            checkAfterIdMode(sourceId, RecordRef.EMPTY)
            checkAfterIdMode(sourceId, RecordRef.valueOf("abc@def"))
            checkAfterIdMode(sourceId, RecordRef.valueOf("app/abc@def"))
        }

        val testJsonQuery = { json: String, expectedAfterId: Boolean ->

            val queryFromJson = ObjectData.create(json)
                .getAs(RecordsQuery::class.java)!!
            assertThat(queryFromJson.isAfterIdMode()).isEqualTo(expectedAfterId)

            checkQuery(queryFromJson, expectedAfterId)
        }

        listOf("test-v1", "test-v0").forEach { sourceId ->
            testJsonQuery(
                """
                {
                    "sourceId": "$sourceId",
                    "page": {
                        "skipCount": 0,
                        "maxItems": 1,
                        "afterId": "app/test@abc"
                    }
                }
            """,
                true
            )
            testJsonQuery(
                """
                {
                    "sourceId": "$sourceId",
                    "page": {
                        "skipCount": 0,
                        "maxItems": 1,
                        "afterId": ""
                    }
                }
            """,
                true
            )
            testJsonQuery(
                """
                {
                    "sourceId": "$sourceId",
                    "page": {
                        "skipCount": 0,
                        "maxItems": 1,
                        "afterId": null
                    }
                }
            """,
                false
            )
            testJsonQuery(
                """
                {
                    "sourceId": "$sourceId",
                    "page": {
                        "skipCount": 0,
                        "maxItems": 1
                    }
                }
            """,
                false
            )
        }
    }

    @Test
    fun queryBodyTest() {

        val factory = RecordsServiceFactory()
        val queries = mutableListOf<RecordsQuery>()

        factory.recordsServiceV1.register(object : RecordsQueryDao {
            override fun getId() = "test"
            override fun queryRecords(recsQuery: RecordsQuery): Any? {
                queries.add(recsQuery)
                return null
            }
        })

        val queryImpl = { version: Int, page: ObjectData ->
            factory.restHandlerAdapter.queryRecords(
                ObjectData.create()
                    .set("version", version)
                    .set(
                        "query",
                        ObjectData.create()
                            .set("sourceId", "test")
                            .set("page", page)
                    )
            )
        }

        val page0 = ObjectData.create()
            .set("skipCount", 10)
            .set("afterId", "")
            .set("maxItems", 10)

        queryImpl(1, page0)
        queryImpl(2, page0)

        assertThat(queries).hasSize(2)
        assertThat(queries[0].isAfterIdMode()).isFalse
        assertThat(queries[0].page.skipCount).isEqualTo(10)
        assertThat(queries[1].isAfterIdMode()).isTrue
        assertThat(queries[1].page.skipCount).isEqualTo(0)

        queries.clear()

        val page1 = ObjectData.create()
            .set("skipCount", 10)
            .set("afterId", "someId")
            .set("maxItems", 10)

        queryImpl(1, page1)
        queryImpl(2, page1)

        assertThat(queries).hasSize(2)
        assertThat(queries[0].isAfterIdMode()).isTrue
        assertThat(queries[0].page.skipCount).isEqualTo(0)
        assertThat(queries[1].isAfterIdMode()).isTrue
        assertThat(queries[1].page.skipCount).isEqualTo(0)
    }

    @Test
    fun queryPageTest() {

        val page = QueryPage.create()
            .withMaxItems(100)
            .withSkipCount(10)
            .build()
        val json = Json.mapper.convert(page, ObjectData::class.java)!!

        assertThat(json.size()).isEqualTo(2)
        assertThat(json.has("afterId")).isFalse
        assertThat(json.get("maxItems").asInt()).isEqualTo(100)
        assertThat(json.get("skipCount").asInt()).isEqualTo(10)

        val page2 = QueryPage.create()
            .withAfterId(RecordRef.EMPTY)
            .build()
        val json2 = Json.mapper.convert(page2, ObjectData::class.java)!!

        assertThat(json2.size()).isEqualTo(3)
        assertThat(json2.has("afterId")).isTrue
        assertThat(json2.get("afterId").asText()).isEqualTo("")
        assertThat(json2.get("maxItems").asInt()).isEqualTo(-1)
        assertThat(json2.get("skipCount").asInt()).isEqualTo(0)

        val ref = RecordRef.create("emodel", "type", "abc")
        val page3 = QueryPage.create()
            .withAfterId(ref)
            .build()
        val json3 = Json.mapper.convert(page3, ObjectData::class.java)!!

        assertThat(json3.size()).isEqualTo(3)
        assertThat(json3.has("afterId")).isTrue
        assertThat(json3.get("afterId").asText()).isEqualTo(ref.toString())
        assertThat(json3.get("maxItems").asInt()).isEqualTo(-1)
        assertThat(json3.get("skipCount").asInt()).isEqualTo(0)
    }
}
