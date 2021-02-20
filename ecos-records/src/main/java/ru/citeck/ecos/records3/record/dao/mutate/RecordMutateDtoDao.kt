package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordMutateDtoDao<T> : RecordsDao {

    /**
     * @param recordId identifier of mutated record
     */
    fun getRecToMutate(recordId: String): T

    /**
     * @return identifier of mutated record.
     *         Can be different value than recordId in getRecToMutate argument
     */
    fun saveMutatedRec(record: T): String
}
