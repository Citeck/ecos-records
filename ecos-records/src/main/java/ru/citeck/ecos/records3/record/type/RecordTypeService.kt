package ru.citeck.ecos.records3.record.type

import ru.citeck.ecos.webapp.api.entity.EntityRef

interface RecordTypeService {

    fun getRecordType(typeRef: EntityRef): RecordTypeInfo
}
