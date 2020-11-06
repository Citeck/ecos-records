package ru.citeck.ecos.records3.spring.utils.registrar

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.RecordsDaoRegistrar
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.RecordsDao
import javax.annotation.PostConstruct

@Component
class RecordsDaoRegistrarComponent @Autowired constructor(
    recordsService: RecordsService,
    recordsServiceV0: ru.citeck.ecos.records2.RecordsService
) : RecordsDaoRegistrar(recordsService, recordsServiceV0) {

    @PostConstruct
    fun init() {
        register()
    }

    @Autowired(required = false)
    override fun setSources(sources: List<RecordsDao>?) {
        super.setSources(sources)
    }

    @Autowired(required = false)
    override fun setSourcesV0(sourcesV0: List<ru.citeck.ecos.records2.source.dao.RecordsDao>?) {
        super.setSourcesV0(sourcesV0)
    }
}
