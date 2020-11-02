package ru.citeck.ecos.records3.record.op.query.dto

import ru.citeck.ecos.commons.json.Json
import java.util.*
import kotlin.math.max

open class RecsQueryRes<T : Any>() {

    companion object {

        @JvmStatic
        @SafeVarargs
        fun <T : Any> of(vararg values: T?): RecsQueryRes<T> {
            val result = RecsQueryRes<T>()
            result.setRecords(listOf(*values))
            return result
        }
    }

    private var records: MutableList<T> = ArrayList()
    private var hasMore = false
    private var totalCount: Long = 0

    constructor(other: RecsQueryRes<T>) : this() {
        hasMore = other.hasMore
        totalCount = other.totalCount
        setRecords(other.records)
    }

    constructor(records: List<T>) : this() {
        setRecords(records)
    }

    fun <K : Any> withRecords(mapper: (T) -> K?): RecsQueryRes<K> {
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

    fun setTotalCount(totalCount: Long) {
        this.totalCount = totalCount
    }

    fun getTotalCount() = max(totalCount, records.size.toLong())

    fun setHasMore(hasMore: Boolean) {
        this.hasMore = hasMore
    }

    fun getHasMore(): Boolean = hasMore

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
