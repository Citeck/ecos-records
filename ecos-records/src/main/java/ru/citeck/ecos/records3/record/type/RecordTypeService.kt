package ru.citeck.ecos.records3.record.type

import ru.citeck.ecos.webapp.api.entity.EntityRef

interface RecordTypeService {

    fun getSourceId(type: String): String {
        val typeRef = EntityRef.valueOf(type).withDefault(
            appName = "emodel",
            sourceId = "type"
        )
        return getRecordType(typeRef).getSourceId()
    }

    fun getRecordType(typeRef: EntityRef): RecordTypeInfo
}
