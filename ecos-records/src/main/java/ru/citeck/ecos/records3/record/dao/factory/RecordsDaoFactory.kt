package ru.citeck.ecos.records3.record.dao.factory

import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordsDaoFactory<T : Any> {

    fun create(id: String, config: T): RecordsDao

    fun getType(): String
}
