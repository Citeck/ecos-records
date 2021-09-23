package ru.citeck.ecos.records3.record.atts.value

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType

class RecordAttValueCtx(
    val record: Any,
    val service: RecordsService
) : AttValueCtx {

    private val recordRef: RecordRef by lazy {
        RecordRef.valueOf(service.getAtt(record, ScalarType.ID.schema).asText())
    }

    override fun getValue(): Any {
        return record
    }

    override fun getRef(): RecordRef {
        return recordRef
    }

    override fun getLocalId(): String {
        return recordRef.id
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
