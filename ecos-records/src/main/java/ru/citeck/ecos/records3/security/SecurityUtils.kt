package ru.citeck.ecos.records3.security

object SecurityUtils {

    fun <T> withoutSensitiveData(value: List<T>): List<T> {
        return value.map { withoutSensitiveData(it) }
    }

    fun <T> withoutSensitiveData(value: T): T {
        if (value is HasSensitiveData<*>) {
            @Suppress("UNCHECKED_CAST")
            return value.withoutSensitiveData() as T
        }
        return value
    }
}
