package ru.citeck.ecos.records3.test.record.dao.impl.ext

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.data.SimpleAuthData
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDao
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDaoConfig
import ru.citeck.ecos.records3.record.dao.impl.ext.impl.ReadOnlyMapExtStorage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.workspace.RecordsWorkspaceService
import ru.citeck.ecos.webapp.api.entity.EntityRef

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

    @ParameterizedTest
    @CsvSource(value = [
        "false, 'user0',  'rec1;rec2;rec3;rec4;rec5', ''",
        "false, 'user1',  'rec1;rec2;rec3;rec4;rec5', ''",
        "false, 'user2',  'rec1;rec2;rec3;rec4;rec5', ''",
        "false, 'system', 'rec1;rec2;rec3;rec4;rec5', ''",
        "true,  'user0', 'rec1;rec2;rec5',            'rec3;rec4'",
        "true,  'user1', 'rec1;rec2;rec3;rec5',       'rec4'",
        "true,  'user2', 'rec5',                      'rec1;rec2;rec3;rec4'",
        "true, 'system', 'rec1;rec2;rec3;rec4;rec5',  ''"
    ])
    fun testWithWorkspaces(workspaceScoped: Boolean, user: String, allowedRecs: String, disallowedRecs: String) {

        val recordsMap = mapOf(
            "rec1" to RecordInWs("rec1", "ws0"),
            "rec2" to RecordInWs("rec2", "ws0"),
            "rec3" to RecordInWs("rec3", "ws1"),
            "rec4" to RecordInWs("rec4", "ws2"),
            "rec5" to RecordInWs("rec5", "")
        )
        val userWorkspaces = mapOf(
            "user0" to setOf("ws0"),
            "user1" to setOf("ws0", "ws1"),
            "user2" to setOf()
        )

        val workspaceService = object : RecordsWorkspaceService {
            override fun isWorkspaceWithGlobalEntities(workspace: String?): Boolean {
                return workspace.isNullOrBlank()
            }
            override fun getUserWorkspaces(user: String): Set<String> {
                return userWorkspaces[user] ?: emptySet()
            }
        }
        val serviceFactory = RecordsServiceFactory()
        serviceFactory.setCustomRecordsWorkspaceService(workspaceService)

        val records = serviceFactory.recordsService
        val daoConfig = ExtStorageRecordsDaoConfig.create(ReadOnlyMapExtStorage(recordsMap))
            .withEcosType("customType")
            .withWorkspaceScoped(workspaceScoped)
            .withSourceId("test")
            .build()
        records.register(ExtStorageRecordsDao(daoConfig))

        val allowedRecsSet = allowedRecs.split(";").toSet()
        val disallowedRecsSet = disallowedRecs.split(";").toSet()

        val recsQuery = RecordsQuery.create()
            .withQuery(Predicates.alwaysTrue())
            .withSourceId("test")
            .build()

        val authorities = if (user == "system") AuthContext.SYSTEM_AUTH.getAuthorities() else emptyList()
        val runAsAuth = SimpleAuthData(user, authorities)
        val recordsRes = AuthContext.runAs(runAsAuth) {
            records.query(recsQuery)
        }
        assertThat(recordsRes.getRecords().map { it.getLocalId() }).containsExactlyInAnyOrderElementsOf(allowedRecsSet)
        assertThat(recordsRes.getRecords().map { it.getLocalId() }).doesNotContainAnyElementsOf(disallowedRecsSet)

        val queryWithWorkspacesWsList = listOf("ws0", "ws2")
        val queryWithWsRes = AuthContext.runAs(runAsAuth) {
            records.query(recsQuery.copy().withWorkspaces(queryWithWorkspacesWsList).build())
        }

        val disallowedRecsWithCustomWs = disallowedRecsSet.toMutableSet()
        val allowedRecsFilteredSet = allowedRecsSet.filter {
            if (!workspaceScoped || queryWithWorkspacesWsList.contains(recordsMap[it]!!.workspace)) {
                true
            } else {
                disallowedRecsWithCustomWs.add(it)
                false
            }
        }
        assertThat(queryWithWsRes.getRecords().map { it.getLocalId() }).containsExactlyInAnyOrderElementsOf(allowedRecsFilteredSet)
        assertThat(queryWithWsRes.getRecords().map { it.getLocalId() }).doesNotContainAnyElementsOf(disallowedRecsWithCustomWs)

        val allRefs = recordsMap.map { EntityRef.create("test", it.key) }
        val allAttsRes = AuthContext.runAs(runAsAuth) {
            records.getAtts(allRefs, listOf("workspace"))
        }
        allAttsRes.forEachIndexed { idx, atts ->
            val id = allRefs[idx].getLocalId()
            val workspace = atts.getAtt("workspace").asText()
            if (allowedRecsSet.contains(id)) {
                val expectedRec = recordsMap[id]
                assertNotNull(expectedRec)
                assertThat(workspace).isEqualTo(expectedRec.workspace)
            } else if (disallowedRecs.contains(id)) {
                assertThat(workspace).isEmpty()
            } else {
                fail("Unknown id: '$id'")
            }
        }
    }

    data class RecordInWs(
        val id: String,
        val workspace: String
    )
}
