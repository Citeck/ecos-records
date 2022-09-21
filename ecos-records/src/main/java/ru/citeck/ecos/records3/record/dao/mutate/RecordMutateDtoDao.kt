package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordMutateDtoDao<T> : RecordsDao {

    /**
     * @param recordId identifier of mutated record
     */
    @Throws(Exception::class)
    fun getRecToMutate(recordId: String): T

    /**
     * @return identifier of mutated record.
     *         Can be different value than recordId in getRecToMutate argument
     */
    @Throws(Exception::class)
    fun saveMutatedRec(record: T): String
}
