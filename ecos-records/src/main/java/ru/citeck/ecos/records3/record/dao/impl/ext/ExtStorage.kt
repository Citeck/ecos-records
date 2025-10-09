package ru.citeck.ecos.records3.record.dao.impl.ext

interface ExtStorage<T : Any> {

    fun getByIds(ids: List<String>): List<T?>

    fun getById(id: String): T?

    fun getIds(): List<String>

    fun getValues(): List<T>

    fun getAll(): Map<String, T>
}
