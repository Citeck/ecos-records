package ru.citeck.ecos.records3.cache.stats

data class CacheKeyHits<K>(
    val key: K,
    val hits: Long
)
