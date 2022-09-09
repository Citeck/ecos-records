package ru.citeck.ecos.records3

import mu.KotlinLogging
import ru.citeck.ecos.records3.record.dao.RecordsDao

open class RecordsDaoRegistrar constructor(
    services: RecordsServiceFactory
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val recordsService: RecordsService = services.recordsServiceV1
    private val recordsServiceV0: ru.citeck.ecos.records2.RecordsService = services.recordsService

    private var sources: List<RecordsDao>? = null
    private var sourcesV0: List<ru.citeck.ecos.records2.source.dao.RecordsDao>? = null

    fun register() {
        log.info("========================== RecordsDaoRegistrar ==========================")
        if (sources != null) {
            sources!!.forEach { this.register(it) }
        }
        if (sourcesV0 != null) {
            sourcesV0!!.forEach { this.register(it) }
        }
        log.info("========================= /RecordsDaoRegistrar ==========================")
    }

    private fun register(dao: RecordsDao) {
        log.info("Register: \"" + dao.getId() + "\" with class " + dao.javaClass.name)
        recordsService.register(dao)
    }

    private fun register(dao: ru.citeck.ecos.records2.source.dao.RecordsDao) {
        log.info("Register: \"" + dao.id + "\" with class " + dao.javaClass.name)
        recordsServiceV0.register(dao)
    }

    open fun setSources(sources: List<RecordsDao>?) {
        this.sources = sources
    }

    open fun setSourcesV0(sourcesV0: List<ru.citeck.ecos.records2.source.dao.RecordsDao>?) {
        this.sourcesV0 = sourcesV0
    }
}
