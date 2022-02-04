package ru.citeck.ecos.records3.cache

import ru.citeck.ecos.records3.cache.stats.CacheKeyError
import ru.citeck.ecos.records3.cache.stats.CacheKeyHits
import ru.citeck.ecos.records3.cache.stats.CacheStats
import java.lang.Integer.max
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.math.min

/**
 * Fast cache based on map with external update behaviour
 * Used when fast response is more important than consistency
 *
 * For internal usage!
 */
class CacheImpl<K : Any, V>(
    private val config: CacheConfig,
    defaultValue: V,
    private val loader: (K) -> V
) : Cache<K, V> {

    private val data = ConcurrentHashMap<K, CacheValue<V>>()
    private val updatingInProgress = AtomicBoolean()

    private val defaultCacheValue = CacheValue(
        0L,
        defaultValue
    )
    private val errorsSleepPolicy = config.errorsSleepPolicy.ifEmpty { listOf(1L) }

    private val maxAllowedSizeBeforeCleanInGet = max(50, config.maxItems) * 2
    private var removedItemsCount = AtomicLong()

    override fun remove(key: K) {
        data.remove(key)
    }

    override fun get(key: K): V {
        val result = data.computeIfAbsent(key) {
            load(it, defaultCacheValue)
        }
        result.hitsCount.incrementAndGet()
        // in normal mode this condition should not be calculated to true
        // because cleaning by maxItems should be performed by job in update method
        // but in case of some errors or attacks on cache by spam we should
        // be ready to protect system against it
        if (data.size > maxAllowedSizeBeforeCleanInGet) {
            exclusiveUpdate(0, {}) {
                cleanByMaxItems()
            }
        }
        return result.value
    }

    override fun update() {
        exclusiveUpdate(200, {}) {
            cleanByMaxItems()
            updateValues()
        }
    }

    override fun clear() {
        exclusiveUpdate(
            TimeUnit.SECONDS.toMillis(30),
            {
                error("Timeout error. Clearing can't be performed")
            }
        ) {
            removedItemsCount.addAndGet(data.size.toLong())
            data.clear()
        }
    }

    override fun size(): Int {
        return data.size
    }

    override fun getStats(): CacheStats<K> {

        val queueByHits = PriorityQueue<Map.Entry<K, CacheValue<V>>> { e0, e1 ->
            e1.value.hitsCount.get().compareTo(e0.value.hitsCount.get())
        }
        val errors = ArrayList<CacheKeyError<K>>()
        data.entries.forEach {
            queueByHits.add(it)
            if (it.value.errorTime != 0L) {
                errors.add(
                    CacheKeyError(it.key, it.value.loadErrorsCount, it.value.lastErrorMsg)
                )
            }
        }
        val mostHits = ArrayList<CacheKeyHits<K>>()
        repeat(min(10, queueByHits.size)) {
            val entry = queueByHits.poll()
            mostHits.add(CacheKeyHits(entry.key, entry.value.hitsCount.get()))
        }

        return CacheStats(
            mostHits,
            errors,
            removedItemsCount.get()
        )
    }

    private inline fun <T> exclusiveUpdate(waitingTime: Long, timeoutAction: () -> T, action: () -> T): T {
        val startWaitingTime = System.currentTimeMillis()
        while (!updatingInProgress.compareAndSet(false, true)) {
            if ((System.currentTimeMillis() - startWaitingTime) < waitingTime) {
                Thread.sleep(100)
            } else {
                return timeoutAction.invoke()
            }
        }
        try {
            return action.invoke()
        } finally {
            updatingInProgress.set(false)
        }
    }

    private fun updateValues() {
        for ((key, value) in data) {
            if (value.loadErrorsCount > 0) {
                val sleepTime = if (!value.wasLoaded) {
                    config.firstLoadingErrorSleepTime
                } else if (value.loadErrorsCount > errorsSleepPolicy.size) {
                    errorsSleepPolicy.last()
                } else {
                    errorsSleepPolicy[value.loadErrorsCount.toInt() - 1]
                }
                if (System.currentTimeMillis() - value.errorTime >= sleepTime) {
                    data[key] = load(key, value)
                }
            } else if (config.expireAfterWrite >= 0 &&
                (System.currentTimeMillis() - value.writeTime) > config.expireAfterWrite
            ) {
                data[key] = load(key, value)
            }
        }
    }

    private fun cleanByMaxItems() {
        if (config.maxItems == 0) {
            removedItemsCount.addAndGet(data.size.toLong())
            data.clear()
            return
        }
        if (data.size < config.maxItems) {
            return
        }
        val queue = PriorityQueue<Map.Entry<K, CacheValue<V>>> { e0, e1 ->
            e0.value.hitsCount.get().compareTo(e1.value.hitsCount.get())
        }
        data.entries.forEach {
            queue.add(it)
        }
        repeat(max(0, min(data.size - config.maxItems, queue.size))) {
            data.remove(queue.poll().key)
            removedItemsCount.decrementAndGet()
        }
    }

    private fun load(key: K, oldValue: CacheValue<V>): CacheValue<V> {
        try {
            val result = loader(key)
            return oldValue.copy(
                writeTime = System.currentTimeMillis(),
                value = if (result == oldValue.value) {
                    oldValue.value
                } else {
                    result
                },
                errorTime = 0,
                loadErrorsCount = 0,
                lastErrorMsg = "",
                wasLoaded = true
            )
        } catch (e: Exception) {
            return oldValue.copy(
                errorTime = System.currentTimeMillis(),
                loadErrorsCount = oldValue.loadErrorsCount + 1,
                lastErrorMsg = e.message ?: ""
            )
        }
    }
}
