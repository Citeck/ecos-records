package ru.citeck.ecos.records3.record.atts.value

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef

interface AttValueCtx {

    fun getRef(): RecordRef

    fun getLocalId(): String

    fun getAtt(attribute: String): DataValue

    fun getAtts(attributes: Map<String, *>): ObjectData

    fun getAtts(attributes: Collection<String>): ObjectData

    fun <T : Any> getAtts(attributes: Class<T>): T
}