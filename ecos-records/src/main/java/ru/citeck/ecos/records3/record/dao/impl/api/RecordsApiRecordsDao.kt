package ru.citeck.ecos.records3.record.dao.impl.api

import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.cache.CacheManager
import ru.citeck.ecos.records3.cache.stats.CacheStats
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao

class RecordsApiRecordsDao : AbstractRecordsDao(), RecordAttsDao {

    companion object {
        const val ID = "api"
    }

    private lateinit var cacheManager: CacheManager

    override fun getRecordAtts(recordId: String): Any? {
        if (recordId != "") {
            return null
        }
        return Record(
            version = ApiVersion(
                query = 2,
                mutate = 1,
                txn = 1,
                delete = 1
            )
        )
    }

    override fun getId() = ID

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        super.setRecordsServiceFactory(serviceFactory)
        cacheManager = serviceFactory.cacheManager
    }

    private inner class Record(
        val version: ApiVersion
    ) {
        fun getCache(): CacheValue {
            return CacheValue()
        }
    }

    private inner class CacheValue {
        fun getStats(): Map<String, CacheStats<*>> {
            return cacheManager.getStats()
        }
    }

    private class ApiVersion(
        val query: Int,
        val mutate: Int,
        val txn: Int,
        val delete: Int
    )
}
