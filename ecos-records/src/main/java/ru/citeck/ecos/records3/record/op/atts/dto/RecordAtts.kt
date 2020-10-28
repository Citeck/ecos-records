package ru.citeck.ecos.records3.record.op.atts.dto

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import lombok.extern.slf4j.Slf4j
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function

@Slf4j
open class RecordAtts() {

    private var id = RecordRef.EMPTY
    private var attributes = ObjectData.create()

    constructor(other: RecordAtts?) : this() {
        setId(other?.id)
        setAtts(other?.getAtts())
    }

    constructor(other: RecordAtts?, id: RecordRef?) : this() {
        setId(id)
        setAtts(other?.getAtts())
    }

    constructor(other: RecordAtts?, idMapper: Function<RecordRef, RecordRef>) : this() {
        setId(idMapper.apply(other?.id ?: RecordRef.EMPTY))
        setAtts(other?.getAtts())
    }

    constructor(id: String?) : this() {
        setId(id)
    }

    constructor(id: RecordRef?) : this() {
        setId(id)
    }

    constructor(id: RecordRef?, attributes: ObjectData?) : this() {
        setId(id)
        setAtts(attributes)
    }

    fun getId() : RecordRef {
        return id
    }

    open fun withId(recordRef: RecordRef?): RecordAtts {
        return if (id == recordRef) {
            this
        } else {
            RecordAtts(recordRef, attributes)
        }
    }

    open fun withDefaultAppName(appName: String?): RecordAtts {
        if (appName == null) {
            return this
        }
        val currId = id
        val newId = currId.withDefaultAppName(appName)
        return if (newId === currId) this else RecordAtts(this, newId)
    }

    @JsonProperty
    fun setId(id: String?) {
        this.id = RecordRef.valueOf(id)
    }

    fun setId(id: RecordRef?) {
        this.id = RecordRef.valueOf(id)
    }

    fun forEach(consumer: BiConsumer<String, DataValue>) {
        attributes.forEach(consumer)
    }

    fun getAtts() : ObjectData {
        return attributes
    }

    fun setAtts(attributes: ObjectData?) {
        this.attributes = attributes?.deepCopy() ?: ObjectData.create()
    }

    fun hasAtt(name: String?): Boolean {
        if (name == null) {
            return false
        }
        return attributes.has(name)
    }

    @JvmOverloads
    fun fmtDate(name: String?, format: String, orElse: String = ""): String {
        val date = getDateOrNull(name)
        if (date != null) {
            val dateFormat = SimpleDateFormat(format)
            return dateFormat.format(date)
        }
        return orElse
    }

    fun getDateOrNull(name: String?): Date? {
        if (name == null) {
            return null
        }
        return attributes.get(name, Date::class.java, null)
    }

    fun getStringOrNull(name: String?): String? {
        if (name == null) {
            return null
        }
        return attributes.get(name, String::class.java, null)
    }

    fun getDoubleOrNull(name: String?): Double? {
        if (name == null) {
            return null
        }
        return attributes.get(name, Double::class.java)
    }

    fun getBoolOrNull(name: String?): Boolean? {
        if (name == null) {
            return null
        }
        return attributes.get(name, Boolean::class.java)
    }

    fun getAtt(name: String?): DataValue {
        if (name == null) {
            return DataValue.NULL
        }
        return attributes.get(name)
    }

    fun <T : Any> getAtt(name: String?, orElse: T): T  {
        if (name == null) {
            return orElse
        }
        return attributes.get(name, orElse)
    }

    fun setAtt(name: String?, value: Any?) {
        if (name == null) {
            return
        }
        attributes.set(name, value)
    }

    override fun toString(): String {
        return Json.mapper.toString(this) ?: "RecordAtts"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as RecordAtts
        return (id == that.id  && attributes == that.attributes)
    }

    override fun hashCode(): Int {
        return Objects.hash(id, attributes)
    }
}
