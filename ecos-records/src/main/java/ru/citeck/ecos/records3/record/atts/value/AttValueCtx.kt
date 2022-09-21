package ru.citeck.ecos.records3.record.atts.value

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef

interface AttValueCtx {

    /**
     * Get internal raw value from this context.
     * Returned value should be ready to call recordsService.getAtts(value, atts)
     */
    fun getValue(): Any

    fun getRef(): RecordRef

    fun getRawRef(): EntityRef

    fun getLocalId(): String

    fun getAtt(attribute: String): DataValue

    fun getAtts(attributes: Map<String, *>): ObjectData

    fun getAtts(attributes: Collection<String>): ObjectData

    fun <T : Any> getAtts(attributes: Class<T>): T
}
