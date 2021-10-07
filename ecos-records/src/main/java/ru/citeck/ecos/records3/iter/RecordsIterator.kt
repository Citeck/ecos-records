package ru.citeck.ecos.records3.iter

interface RecordsIterator<T> : Iterator<T> {

    fun getProcessedCount(): Long

    fun getTotalCount(): Long
}
