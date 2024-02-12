package ru.citeck.ecos.records3.test.record.mixin.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.mixin.external.ExtAttMixinConfigurer
import ru.citeck.ecos.records3.record.mixin.external.ExtMixinConfig
import ru.citeck.ecos.records3.record.mixin.external.remote.ExtAttRemoteConfigurer
import ru.citeck.ecos.records3.record.mixin.external.remote.ExtAttRemoteRegistry
import ru.citeck.ecos.records3.record.mixin.external.remote.ExtAttRemoteRegistryData
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ExtAttMixinTest {

    @Test
    fun test() {
        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("rec-0", Record(0))
                .build()
        )

        services.extAttMixinService.register(object : ExtAttMixinConfigurer {
            override fun configure(settings: ExtMixinConfig) {
                settings.setEcosType("test-type")
                settings.addProvidedAtt("evaluated")
                    .addRequiredAtts(mapOf("valueAlias" to "value"))
                    .withHandler { "prefix-" + it["valueAlias"].asText() }
                settings.addProvidedAtt("evaluatedWithDto")
                    .addRequiredAtts(MixinAttsTest::class.java)
                    .withHandler(MixinAttsTest::class.java) {
                        "prefix2-" + it.customField
                    }
                settings.addProvidedAtt("attWithAttValue")
                    .addRequiredAtts(MixinAttsTest::class.java)
                    .withHandler(MixinAttsTest::class.java) {
                        object : AttValue {
                            override fun getAtt(name: String): Any? {
                                if (name == "inner") {
                                    return "abc-" + it.customField
                                }
                                return null
                            }
                        }
                    }
            }
        })

        assertThat(records.getAtt("test@rec-0", "evaluated").asText()).isEqualTo("prefix-value-0")
        assertThat(records.getAtt("test@rec-0", "evaluatedWithDto").asText()).isEqualTo("prefix2-value-0")
        assertThat(records.getAtt("test@rec-0", "attWithAttValue.inner").asText()).isEqualTo("abc-value-0")
    }

    @Test
    fun remoteTest() {

        val appsFactory = MockAppsFactory()
        val app0 = appsFactory.createApp("app-0")
        val app1 = appsFactory.createApp("app-1")

        app0.factory.extAttMixinService.register(object : ExtAttMixinConfigurer {
            override fun configure(settings: ExtMixinConfig) {
                settings.setEcosType("test-type")
                settings.addProvidedAtt("localAtt")
                    .addRequiredAtts(mapOf("valueAlias2" to "value"))
                    .withHandler {
                        "local-prefix-" + it["valueAlias2"].asText()
                    }
            }
        })

        app1.factory.extAttMixinService.register(object : ExtAttMixinConfigurer {
            override fun configure(settings: ExtMixinConfig) {
                settings.setEcosType("test-type")
                settings.addProvidedAtt("evaluated")
                    .addRequiredAtts(mapOf("valueAlias" to "value"))
                    .withHandler { "prefix-" + it["valueAlias"].asText() }
                settings.addProvidedAtt("evaluatedWithDto")
                    .addRequiredAtts(MixinAttsTest::class.java)
                    .withHandler(MixinAttsTest::class.java) {
                        "prefix2-" + it.customField
                    }
                settings.addProvidedAtt("attWithAttValue")
                    .addRequiredAtts(MixinAttsTest::class.java)
                    .withHandler(MixinAttsTest::class.java) {
                        object : AttValue {
                            override fun getAtt(name: String): Any? {
                                if (name == "inner") {
                                    return "abc-" + it.customField
                                }
                                return null
                            }
                        }
                    }
                settings.addProvidedAtt("refValue")
                    .addRequiredAtts(MixinAttsTest::class.java)
                    .withHandler(MixinAttsTest::class.java) {
                        EntityRef.create("app-0", "test", "rec-111")
                    }
            }
        })

        val remoteAppsRegistryData = app1.factory.extAttMixinService
            .getNonLocalAtts().values.flatMap { it.values }.map {
                ExtAttRemoteRegistryData(it.typeId, it.priority, it.attribute, it.requiredAtts.values.toSet())
            }

        app0.factory.extAttMixinService.register(
            ExtAttRemoteConfigurer(
                object : ExtAttRemoteRegistry {
                    override fun getRemoteData(): Map<String, List<ExtAttRemoteRegistryData>> {
                        return mapOf(app1.name to remoteAppsRegistryData)
                    }
                    override fun onRemoteDataUpdated(action: () -> Unit) {}
                },
                app0.factory.getEcosWebAppApi()!!.getWebClientApi()
            )
        )

        val app0Records = app0.factory.recordsServiceV1
        app0Records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("rec-0", Record(0))
                .addRecord("rec-111", Record(111))
                .build()
        )

        assertThat(app0Records.getAtt("test@rec-0", "evaluated").asText()).isEqualTo("prefix-value-0")
        assertThat(app0Records.getAtt("test@rec-0", "evaluatedWithDto").asText()).isEqualTo("prefix2-value-0")
        assertThat(app0Records.getAtt("test@rec-0", "attWithAttValue.inner").asText()).isEqualTo("abc-value-0")

        val attsRes = app0Records.getAtts(
            "test@rec-0",
            mapOf(
                // _self to exclude cache
                "evaluated" to "evaluated._self",
                "evaluatedWithDto" to "evaluatedWithDto._self",
                "attWithAttValue.inner" to "attWithAttValue.inner._self",
                "localAtt" to "localAtt._self",
                "refValue" to "refValue.value"
            )
        ).getAtts()

        assertThat(attsRes["evaluated"].asText()).isEqualTo("prefix-value-0")
        assertThat(attsRes["evaluatedWithDto"].asText()).isEqualTo("prefix2-value-0")
        assertThat(attsRes["attWithAttValue.inner"].asText()).isEqualTo("abc-value-0")
        assertThat(attsRes["localAtt"].asText()).isEqualTo("local-prefix-value-0")
        assertThat(attsRes["refValue"].asText()).isEqualTo("value-111")
    }

    class MixinAttsTest(
        @AttName("value")
        val customField: String
    )

    class Record(private val idx: Int) {

        fun getValue(): String {
            return "value-$idx"
        }

        fun getEcosType(): String {
            return "test-type"
        }
    }
}