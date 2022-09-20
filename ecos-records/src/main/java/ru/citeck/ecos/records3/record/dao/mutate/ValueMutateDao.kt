package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface ValueMutateDao<T : Any> : RecordsDao {

    @Throws(Exception::class)
    fun mutate(value: T): Any?
}
