package ru.citeck.ecos.records3.record.dao.impl.ext.impl

import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorage
import java.util.*

class ReadOnlyMapExtStorage<T : Any>(private val map: Map<String, T>) : ExtStorage<T> {

    private val readOnlyView = Collections.unmodifiableMap(map)

    override fun getByIds(ids: List<String>): List<T?> {
        return ids.map { map[it] }
    }

    override fun getById(id: String): T? {
        return map[id]
    }

    override fun getIds(): List<String> {
        return map.keys.toList()
    }

    override fun getValues(): List<T> {
        return map.values.toList()
    }

    override fun getAll(): Map<String, T> {
        return readOnlyView
    }
}
