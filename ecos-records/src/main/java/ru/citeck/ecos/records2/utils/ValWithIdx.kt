package ru.citeck.ecos.records2.utils

class ValWithIdx<T>(val value: T, val idx: Int = 0) {

    fun <K> withValue(mapper: (T) -> K): ValWithIdx<K> {
        return ValWithIdx(mapper.invoke(value), idx)
    }
}
