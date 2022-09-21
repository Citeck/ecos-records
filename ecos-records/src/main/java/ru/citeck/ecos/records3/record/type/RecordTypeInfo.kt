package ru.citeck.ecos.records3.record.type

import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt

interface RecordTypeInfo {

    companion object {
        val EMPTY = object : RecordTypeInfo {
            override fun getSourceId(): String = ""
            override fun getComputedAtts(): List<RecordComputedAtt> = emptyList()
        }
    }

    fun getSourceId(): String

    fun getComputedAtts(): List<RecordComputedAtt>
}
