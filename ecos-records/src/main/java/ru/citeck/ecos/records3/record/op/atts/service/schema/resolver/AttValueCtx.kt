package ru.citeck.ecos.records3.record.op.atts.service.schema.resolver

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef

interface AttValueCtx {

    fun getRef(): RecordRef

    fun getLocalId(): String

    fun getAtt(attribute: String): DataValue

    fun getAtts(attributes: Map<String, *>): ObjectData

    fun <T : Any> getAtts(attributes: Class<T>): T
}
