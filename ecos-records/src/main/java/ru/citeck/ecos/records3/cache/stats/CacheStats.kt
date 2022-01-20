package ru.citeck.ecos.records3.cache.stats

data class CacheStats<K>(
    val mostHits: List<CacheKeyHits<K>>,
    val errors: List<CacheKeyError<K>>,
    val removedItemsCount: Long
)
