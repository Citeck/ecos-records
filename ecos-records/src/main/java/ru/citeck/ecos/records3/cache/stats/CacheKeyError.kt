package ru.citeck.ecos.records3.cache.stats

data class CacheKeyError<K>(
    val key: K,
    val loadErrorsCount: Long,
    val lastErrorMsg: String
)
