package ru.citeck.ecos.records3.cache

import ru.citeck.ecos.records2.source.dao.local.job.PeriodicJob
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.cache.stats.CacheStats
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * For internal usage!
 */
internal class CacheManager(factory: RecordsServiceFactory) {

    private val caches = ConcurrentHashMap<String, CacheImpl<*, *>>()

    init {
        factory.jobExecutor.addSystemJob(object : PeriodicJob {
            override fun getInitDelay(): Long = TimeUnit.SECONDS.toMillis(30)
            override fun getPeriod(): Long = TimeUnit.SECONDS.toMillis(2)
            override fun execute(): Boolean {
                caches.values.forEach { it.update() }
                return false
            }
        })
    }

    fun <K : Any, V> create(config: CacheConfig, defaultValue: V, loader: (K) -> V): Cache<K, V> {
        if (config.maxItems < 0) {
            error("Incorrect maxItems. Should be [0,MAX_INT], but found: ${config.maxItems}")
        }
        val cache = CacheImpl(config, defaultValue, loader)
        caches[config.key] = cache
        return cache
    }

    fun clearAll() {
        caches.values.forEach { it.clear() }
    }

    fun getStats(): Map<String, CacheStats<*>> {
        return caches.entries.associate {
            it.key to it.value.getStats()
        }
    }
}
