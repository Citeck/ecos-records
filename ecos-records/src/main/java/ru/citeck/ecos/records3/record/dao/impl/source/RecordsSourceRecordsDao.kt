package ru.citeck.ecos.records3.record.dao.impl.source

import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.HasSourceIdAliases
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import java.util.stream.Collectors

class RecordsSourceRecordsDao(services: RecordsServiceFactory) :
    AbstractRecordsDao(),
    RecordsQueryDao,
    RecordsAttsDao,
    HasSourceIdAliases {

    companion object {
        const val ID = "src"
        private val aliases = listOf("source")
    }

    private val recordsResolver = services.recordsResolver

    override fun getRecordsAtts(recordIds: List<String>): List<*> {

        return recordIds.stream().map { rec: String ->
            val info = recordsResolver.getSourceInfo(rec) ?: return@map EmptyAttValue.INSTANCE
            info
        }.collect(Collectors.toList())
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any {
        return recordsResolver.getSourcesInfo()
    }

    override fun getId(): String {
        return ID
    }

    override fun getSourceIdAliases(): Collection<String> {
        return aliases
    }
}
