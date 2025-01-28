package ru.citeck.ecos.records3.record.atts.value

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordAttValueCtx(
    val record: Any,
    val service: RecordsService
) : AttValueCtx {

    private val recordRef: EntityRef by lazy {
        if (record is EntityRef) {
            EntityRef.valueOf(record)
        } else {
            EntityRef.valueOf(service.getAtt(record, ScalarType.ID.schema).asText())
        }
    }

    private val typeRefValue: EntityRef by lazy {
        EntityRef.valueOf(service.getAtt(record, RecordConstants.ATT_TYPE + "?id").asText())
    }

    override fun getTypeRef(): EntityRef {
        return typeRefValue
    }

    override fun getTypeId(): String {
        return typeRefValue.getLocalId()
    }

    override fun getValue(): Any {
        return record
    }

    override fun getRef(): EntityRef {
        return recordRef
    }

    override fun getRawRef(): EntityRef {
        return recordRef
    }

    override fun getLocalId(): String {
        return recordRef.getLocalId()
    }

    override fun getAtt(attribute: String): DataValue {
        return service.getAtt(record, attribute)
    }

    override fun getAtts(attributes: Map<String, *>): ObjectData {
        return service.getAtts(record, attributes).getAtts()
    }

    override fun getAtts(attributes: Collection<String>): ObjectData {
        return service.getAtts(record, attributes).getAtts()
    }

    override fun <T : Any> getAtts(attributes: Class<T>): T {
        return service.getAtts(record, attributes)
    }
}
