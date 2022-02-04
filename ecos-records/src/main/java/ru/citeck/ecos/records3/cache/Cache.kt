package ru.citeck.ecos.records3.cache

import ru.citeck.ecos.records3.cache.stats.CacheStats

interface Cache<K : Any, V> {

    fun remove(key: K)

    fun get(key: K): V

    fun update()

    fun size(): Int

    fun clear()

    fun getStats(): CacheStats<K>
}
