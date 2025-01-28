package ru.citeck.ecos.records3.iter

import ru.citeck.ecos.commons.data.ObjectData

interface RecordsIterator<T> : Iterator<T> {

    fun getProcessedCount(): Long

    fun getState(full: Boolean): ObjectData

    fun setState(state: ObjectData)

    fun getTotalCount(): Long
}
