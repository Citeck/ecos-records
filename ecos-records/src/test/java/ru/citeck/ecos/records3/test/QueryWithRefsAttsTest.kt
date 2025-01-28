package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory

class QueryWithRefsAttsTest {

    @Test
    fun test() {

        val appsFactory = MockAppsFactory()
        val app0 = appsFactory.createApp("app0")
        val app1 = appsFactory.createApp("app1")

        val app0Records = app0.factory.recordsServiceV1

        val app0dao0getAttsIds = mutableListOf<List<String>>()
        val app0dao1getAttsIds = mutableListOf<List<String>>()
        val app1dao0getAttsIds = mutableListOf<List<String>>()

        val app0dao0 = TestDao(app0.name, "dao0") { app0dao0getAttsIds.add(it) }
        val app0dao1 = TestDao(app0.name, "dao1") { app0dao1getAttsIds.add(it) }
        val app1dao0 = TestDao(app1.name, "dao0") { app1dao0getAttsIds.add(it) }

        fun clearState() {
            app0dao0getAttsIds.clear()
            app0dao1getAttsIds.clear()
            app1dao0getAttsIds.clear()
        }

        app0Records.register(app0dao0)
        app0Records.register(app0dao1)
        app1.factory.recordsServiceV1.register(app1dao0)

        fun query(dao: TestDao, refs: List<Any>, att: String): List<String> {
            val records = when (dao.appName) {
                app0.name -> app0.factory.recordsServiceV1
                app1.name -> app1.factory.recordsServiceV1
                else -> error("Unknown app: '${dao.appName}'")
            }
            return records.query(
                RecordsQuery.create()
                    .withSourceId(dao.getId())
                    .withQuery(DataValue.createObj().set("refs", refs))
                    .build(),
                listOf(att)
            ).getRecords().map { it[att].asText() }
        }

        val refs0 = listOf("abc", "def")
        val res0 = query(app0dao0, refs0, "key")

        assertThat(app0dao0getAttsIds).containsExactly(refs0)
        assertThat(app0dao1getAttsIds).isEmpty()
        assertThat(app1dao0getAttsIds).isEmpty()
        assertThat(res0).containsExactly("dao0-abc", "dao0-def")

        clearState()

        val res0Refs = query(app0dao0, refs0, "?id")
        assertThat(app0dao0getAttsIds).containsExactly(refs0)
        assertThat(app0dao1getAttsIds).isEmpty()
        assertThat(app1dao0getAttsIds).isEmpty()
        assertThat(res0Refs).containsExactly(
            "${app0.name}/${app0dao0.getId()}@abc",
            "${app0.name}/${app0dao0.getId()}@def"
        )

        clearState()

        val refs1 = listOf(
            "${app0dao1.getId()}@abc",
            "${app0dao1.getId()}@def"
        )
        val res1 = query(app0dao0, refs1, "key")

        assertThat(app0dao0getAttsIds).isEmpty()
        assertThat(app1dao0getAttsIds).isEmpty()
        assertThat(app0dao1getAttsIds).containsExactly(refs1.map { it.substringAfter('@') })
        assertThat(res1).containsExactly("dao1-abc", "dao1-def")

        clearState()

        val refs2 = listOf(
            "${app1.name}/${app1dao0.getId()}@abc",
            "${app1.name}/${app1dao0.getId()}@def"
        )
        val res2 = query(app0dao0, refs2, "key")

        assertThat(app0dao0getAttsIds).isEmpty()
        assertThat(app0dao1getAttsIds).isEmpty()
        assertThat(app1dao0getAttsIds).containsExactly(refs2.map { it.substringAfter('@') })
        assertThat(res2).containsExactly("dao0-abc", "dao0-def")

        clearState()

        val refs3 = listOf(
            "${app1.name}/${app1dao0.getId()}@abc",
            "${app1.name}/${app1dao0.getId()}@def",
            "${app0dao1.getId()}@ghi",
            "${app0dao1.getId()}@jkl",
            "${app0.name}/${app0dao0.getId()}@mno",
            "${app0.name}/${app0dao0.getId()}@pqr",
            "${app0.name}/${app0dao1.getId()}@stu",
            "${app0.name}/${app0dao1.getId()}@vwx",
            "${app0.name}/${app0dao0.getId()}@yz"
        )
        val res3 = query(app0dao0, refs3, "key")

        assertThat(app0dao0getAttsIds).containsExactly(listOf("mno", "pqr", "yz"))
        assertThat(app0dao1getAttsIds).containsExactly(listOf("ghi", "jkl", "stu", "vwx"))
        assertThat(app1dao0getAttsIds).containsExactly(listOf("abc", "def"))
        assertThat(res3).containsExactly(
            "dao0-abc",
            "dao0-def",
            "dao1-ghi",
            "dao1-jkl",
            "dao0-mno",
            "dao0-pqr",
            "dao1-stu",
            "dao1-vwx",
            "dao0-yz"
        )
    }

    class TestDao(
        val appName: String,
        private val id: String,
        private val onGetAtts: (recordIds: List<String>) -> Unit
    ) : RecordsQueryDao, RecordsAttsDao {

        override fun getId(): String {
            return id
        }

        override fun queryRecords(recsQuery: RecordsQuery): Any {
            return recsQuery.getQuery(DataValue::class.java)["refs"].asStrList()
        }

        override fun getRecordsAtts(recordIds: List<String>): List<*> {
            onGetAtts(recordIds)
            return recordIds.map {
                mapOf(
                    "key" to "$id-$it"
                )
            }
        }
    }
}
