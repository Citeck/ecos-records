package ru.citeck.ecos.records3

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.records3.record.dao.RecordsDao

open class RecordsDaoRegistrar constructor(
    services: RecordsServiceFactory
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val recordsService: RecordsService = services.recordsService

    private var sources: List<RecordsDao>? = null

    fun register() {
        log.info { "========================== RecordsDaoRegistrar ==========================" }
        if (sources != null) {
            sources!!.forEach { this.register(it) }
        }
        log.info { "========================= /RecordsDaoRegistrar ==========================" }
    }

    private fun register(dao: RecordsDao) {
        log.info { "Register: \"" + dao.getId() + "\" with class " + dao.javaClass.name }
        recordsService.register(dao)
    }

    open fun setSources(sources: List<RecordsDao>?) {
        this.sources = sources
    }
}
