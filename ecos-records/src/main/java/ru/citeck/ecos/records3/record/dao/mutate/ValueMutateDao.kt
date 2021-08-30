package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface ValueMutateDao<T : Any> : RecordsDao {

    fun mutate(value: T): Any?
}
