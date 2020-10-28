package ru.citeck.ecos.records3.record.op.mutate.dao

import ru.citeck.ecos.records2.RecordRef

interface RecordMutateDtoDao<T> {

    fun getRecToMutate(record: RecordRef): T

    fun saveMutatedRec(record: T)
}
