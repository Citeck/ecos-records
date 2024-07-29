package ru.citeck.ecos.records3.test.script

import org.junit.jupiter.api.BeforeEach
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.script.RecordsScriptService
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

abstract class AbstractRecordsScriptTest {

    lateinit var recordsService: RecordsService
    lateinit var recordsServiceFactory: RecordsServiceFactory
    lateinit var rScript: RecordsScriptService

    lateinit var duneSpice: EntityRef

    companion object {
        const val DEFAULT_ARRAKIS_ID = "arrakis"
        const val DEFAULT_ARRAKIS_NAME = "arrakis"
        const val DEFAULT_ARRAKIS_TEMP = 60.0

        const val DEFAULT_SPICE_INTENSITY = 300.5
        const val DEFAULT_SPICE_WEIGHT = 2.0
        const val DEFAULT_SPICE_NAME = "spice"
    }

    @BeforeEach
    fun init() {
        recordsServiceFactory = RecordsServiceFactory()
        recordsService = recordsServiceFactory.recordsService
        rScript = RecordsScriptService(recordsServiceFactory)

        recordsService.register(InMemDataRecordsDao("dune"))

        duneSpice = createSpice(DEFAULT_SPICE_INTENSITY)

        createSpice(10.0)
        createSpice(15.0)
        createSpice(30.0)
        createSpice(40.0)
    }

    private fun createSpice(intensity: Double): EntityRef {
        return recordsService.create(
            "dune",
            mapOf(
                "name" to DEFAULT_SPICE_NAME,
                "intensity" to intensity,
                "weight" to DEFAULT_SPICE_WEIGHT
            )
        )
    }
}
