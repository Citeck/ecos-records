package ru.citeck.ecos.records3.cache

import java.util.concurrent.atomic.AtomicLong

class CacheValue<T>(
    val writeTime: Long,
    val value: T,
    val hitsCount: AtomicLong = AtomicLong(0),
    val wasLoaded: Boolean = false,
    val loadErrorsCount: Long = 0,
    val errorTime: Long = 0,
    val lastErrorMsg: String = ""
) {
    fun copy(
        writeTime: Long = this.writeTime,
        value: T = this.value,
        wasLoaded: Boolean = this.wasLoaded,
        loadErrorsCount: Long = this.loadErrorsCount,
        errorTime: Long = this.errorTime,
        lastErrorMsg: String = this.lastErrorMsg
    ): CacheValue<T> {
        return CacheValue(
            writeTime,
            value,
            if (value === this.value) {
                AtomicLong(hitsCount.get())
            } else {
                AtomicLong(0)
            },
            wasLoaded,
            loadErrorsCount,
            errorTime,
            lastErrorMsg
        )
    }
}
