package ru.citeck.ecos.records3.record.type

import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt

abstract class RecordTypeInfoAdapter : RecordTypeInfo {
    override fun getSourceId(): String = RecordTypeInfo.EMPTY.getSourceId()
    override fun getComputedAtts(): List<RecordComputedAtt> = RecordTypeInfo.EMPTY.getComputedAtts()
}
