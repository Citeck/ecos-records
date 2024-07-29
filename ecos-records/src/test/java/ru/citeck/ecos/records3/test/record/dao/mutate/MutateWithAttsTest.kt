package ru.citeck.ecos.records3.test.record.dao.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory
import ru.citeck.ecos.webapp.api.entity.toEntityRef

class MutateWithAttsTest {

    private fun baseTest(services: RecordsServiceFactory, targetSrcId: String) {

        val records = services.recordsService

        val result = records.mutateAndGetAtts(
            "$targetSrcId@",
            mapOf("abc" to "def"),
            listOf("abc", "other")
        )
        assertThat(result["abc"].asText()).isEqualTo("def")
        assertThat(result["other"].isNull()).isTrue

        val ref = result.getId()

        val result2 = records.mutateAndGetAtts(ref, mapOf("abc" to "new-value"), listOf("abc"))
        assertThat(result2["abc"].asText()).isEqualTo("new-value")

        val ref2 = records.create(targetSrcId, mapOf("abc" to "hij"))

        val getRecsToMutate = { value: String ->
            listOf(ref, ref2).map {
                RecordAtts(it, ObjectData.create("{\"abc\": \"$value\"}"))
            }
        }

        val mutResult0 = records.mutate(getRecsToMutate("multi-value-0"))
        assertThat(mutResult0).hasSize(2)

        val mutResult1 = records.mutateAndGetAtts(getRecsToMutate("multi-value-1"), emptyList())
        assertThat(mutResult1).hasSize(2)
        assertThat(mutResult1[0].getAtts().isEmpty()).isTrue
        assertThat(mutResult1[1].getAtts().isEmpty()).isTrue

        val attValue2 = "multi-value-2"
        val mutResult2 = records.mutateAndGetAtts(
            getRecsToMutate(attValue2),
            mapOf("abc" to "abc")
        )
        assertThat(mutResult2).hasSize(2)
        assertThat(mutResult2[0]["abc"].asText()).isEqualTo(attValue2)
        assertThat(mutResult2[1].getAtts().isEmpty()).isTrue

        val attValue3 = "multi-value-3"
        val mutResult3 = records.mutateAndGetAtts(
            getRecsToMutate(attValue3),
            listOf(
                mapOf("abc" to "abc"),
                mapOf("abc" to "abc")
            )
        )
        assertThat(mutResult3).hasSize(2)
        assertThat(mutResult3[0]["abc"].asText()).isEqualTo(attValue3)
        assertThat(mutResult3[1]["abc"].asText()).isEqualTo(attValue3)
    }

    private fun multipleDaoMutateTest(services: RecordsServiceFactory, targetSourcesId: List<String>) {

        val records = services.recordsService

        var recIdx = 0
        val recsToMutate = targetSourcesId.flatMap { sourceId ->
            listOf(
                RecordAtts("$sourceId@".toEntityRef(), ObjectData.create("""{"$recIdx":"${recIdx++}"}""")),
                RecordAtts("$sourceId@".toEntityRef(), ObjectData.create("""{"$recIdx":"${recIdx++}"}"""))
            )
        }.toMutableList()

        recsToMutate.add(
            RecordAtts(
                "${targetSourcesId[1]}@".toEntityRef(),
                ObjectData.create("""{"$recIdx":"${recIdx++}"}""")
            )
        )
        recsToMutate.add(
            RecordAtts(
                "${targetSourcesId[1]}@".toEntityRef(),
                ObjectData.create("""{"$recIdx":"${recIdx++}"}""")
            )
        )

        val attsToLoad = recsToMutate.map {
            mapOf(it.getAtts().fieldNamesList().first() to it.getAtts().fieldNamesList().first())
        }

        val mutResult = records.mutateAndGetAtts(recsToMutate, attsToLoad)
        assertThat(mutResult.map { it.getId().getSourceId() })
            .containsExactlyElementsOf(recsToMutate.map { it.getId().getSourceId() })

        assertThat(mutResult.map { it.getAtts() }).containsExactlyElementsOf(recsToMutate.map { it.getAtts() })

        val recsAtts = records.getAtts(
            mutResult.map { it.getId() },
            recsToMutate.indices.map { it.toString() }
        )
        for ((idx, atts) in recsAtts.withIndex()) {
            assertThat(atts[idx.toString()].asText()).isEqualTo(idx.toString())
        }
    }

    @Test
    fun localTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsService

        records.register(InMemDataRecordsDao("test"))

        baseTest(services, "test")

        records.register(InMemDataRecordsDao("test-1"))
        records.register(InMemDataRecordsDao("test-2"))
        records.register(InMemDataRecordsDao("test-3"))

        multipleDaoMutateTest(services, listOf("test-1", "test-2", "test-3"))
    }

    @Test
    fun remoteMutWithAttsTest() {

        val appsFactory = MockAppsFactory()
        val app0 = appsFactory.createApp("app-0")
        val app1 = appsFactory.createApp("app-1")

        app1.factory.recordsService.register(InMemDataRecordsDao("test-0"))

        baseTest(app0.factory, "app-1/test-0")

        app1.factory.recordsService.register(InMemDataRecordsDao("test-1"))
        app1.factory.recordsService.register(InMemDataRecordsDao("test-2"))
        app1.factory.recordsService.register(InMemDataRecordsDao("test-3"))

        multipleDaoMutateTest(app0.factory, listOf("app-1/test-1", "app-1/test-2", "app-1/test-3"))
        multipleDaoMutateTest(app1.factory, listOf("app-1/test-1", "app-1/test-2", "app-1/test-3"))
    }
}
