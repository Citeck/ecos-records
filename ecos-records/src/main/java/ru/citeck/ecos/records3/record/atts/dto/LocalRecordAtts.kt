package ru.citeck.ecos.records3.record.atts.dto

import lombok.extern.slf4j.Slf4j
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.security.HasSensitiveData
import java.util.function.BiConsumer

@Slf4j
data class LocalRecordAtts(
    val id: String = "",
    val attributes: ObjectData = ObjectData.create()
) : HasSensitiveData<LocalRecordAtts> {

    constructor(atts: RecordAtts) : this(atts.getId().getLocalId(), atts.getAtts().deepCopy())

    fun withId(id: String): LocalRecordAtts {
        return if (this.id == id) {
            this
        } else {
            LocalRecordAtts(id, attributes.deepCopy())
        }
    }

    fun forEach(consumer: (String, DataValue) -> Unit) {
        attributes.forEach(consumer)
    }

    fun forEachJ(consumer: BiConsumer<String, DataValue>) {
        attributes.forEachJ(consumer)
    }

    fun hasAtt(name: String?): Boolean {
        if (name == null) {
            return false
        }
        return attributes.has(name)
    }

    fun getAtt(name: String?): DataValue {
        if (name == null) {
            return DataValue.NULL
        }
        return attributes.get(name)
    }

    fun <T : Any> getAtt(name: String?, orElse: T): T {
        if (name == null) {
            return orElse
        }
        return attributes.get(name, orElse)
    }

    fun setAtt(name: String?, value: Any?) {
        if (name == null) {
            return
        }
        attributes[name] = value
    }

    fun getAtts(): ObjectData {
        return attributes
    }

    override fun withoutSensitiveData(): LocalRecordAtts {
        val newAtts = attributes.deepCopy()
        newAtts.fieldNamesList().forEach { name ->
            newAtts[name] = "?"
        }
        return LocalRecordAtts(id, newAtts)
    }

    fun deepCopy(): LocalRecordAtts {
        return LocalRecordAtts(id, attributes.deepCopy())
    }

    override fun toString(): String {
        return Json.mapper.toString(this) ?: "LocalRecordAtts"
    }
}
