package ru.citeck.ecos.records3.cache

import java.util.concurrent.TimeUnit

class CacheConfig(
    val key: String,
    val expireAfterWrite: Long = -1,
    val firstLoadingErrorSleepTime: Long = TimeUnit.SECONDS.toMillis(10),
    val errorsSleepPolicy: List<Long> = listOf(
        TimeUnit.SECONDS.toMillis(1),
        TimeUnit.SECONDS.toMillis(5),
        TimeUnit.SECONDS.toMillis(10),
        TimeUnit.SECONDS.toMillis(30),
        TimeUnit.MINUTES.toMillis(2),
        TimeUnit.MINUTES.toMillis(10),
        TimeUnit.MINUTES.toMillis(30),
        TimeUnit.HOURS.toMillis(1)
    ),
    val maxItems: Int
)
