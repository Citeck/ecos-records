package ru.citeck.ecos.records3.record.op.query.dto

import ru.citeck.ecos.commons.json.Json
import java.util.*
import kotlin.math.max

open class RecsQueryRes<T : Any>() {

    companion object {

        @SafeVarargs
        fun <T : Any> of(vararg values: T?): RecsQueryRes<T> {
            val result = RecsQueryRes<T>()
            result.setRecords(listOf(*values))
            return result
        }
    }

    private var records: MutableList<T> = ArrayList()

    var hasMore = false
    var totalCount: Long = 0
        get() = max(field, getRecords().size.toLong())

    constructor(other: RecsQueryRes<T>) {
        hasMore = other.hasMore
        totalCount = other.totalCount
        setRecords(other.records)
    }

    constructor(records: List<T>) {
        setRecords(records)
    }

    fun <K : Any> withRecords(mapper: (T) -> K?) : RecsQueryRes<K> {
        val res = RecsQueryRes(records.mapNotNull(mapper))
        res.hasMore = hasMore
        res.totalCount = totalCount
        return res
    }

    fun merge(other: RecsQueryRes<T>) {
        hasMore = hasMore || other.hasMore
        totalCount += other.totalCount
        addRecords(other.records)
    }

    fun getRecords(): List<T> {
        return records
    }

    fun setRecords(records: List<T?>?) {
        this.records = ArrayList()
        addRecords(records)
    }

    fun addRecord(record: T?) {
        record ?: return
        records.add(record)
    }

    fun addRecords(records: Collection<T?>?) {
        records?.filterNotNull()?.forEach {
            this.records.add(it)
        }
    }

    override fun toString(): String {
        return Json.mapper.toString(this) ?: "RecsQueryRes"
    }
}
