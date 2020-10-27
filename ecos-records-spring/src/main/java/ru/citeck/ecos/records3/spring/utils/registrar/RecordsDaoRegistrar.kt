package ru.citeck.ecos.records3.spring.utils.registrar

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.RecordsDao
import javax.annotation.PostConstruct

@Component
class RecordsDaoRegistrar @Autowired constructor(
    private val recordsService: RecordsService,
    private val recordsServiceV0: ru.citeck.ecos.records2.RecordsService
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private var sources: List<RecordsDao>? = null
    private var sourcesV0: List<ru.citeck.ecos.records2.source.dao.RecordsDao>? = null

    @PostConstruct
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

    @Autowired(required = false)
    fun setSources(sources: List<RecordsDao>?) {
        this.sources = sources
    }

    @Autowired(required = false)
    fun setSourcesV0(sourcesV0: List<ru.citeck.ecos.records2.source.dao.RecordsDao>?) {
        this.sourcesV0 = sourcesV0
    }
}
