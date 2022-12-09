package ru.citeck.ecos.records3.record.dao.impl.ext

interface ExtStorage<T : Any> {

    fun getById(id: String): T?

    fun getIds(): List<String>

    fun getValues(): List<T>

    fun getAll(): Map<String, T>
}
