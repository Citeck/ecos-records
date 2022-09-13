package ru.citeck.ecos.records3.record.type

import ru.citeck.ecos.webapp.api.entity.EntityRef

interface RecordTypeComponent {

    fun getRecordType(typeRef: EntityRef): RecordTypeInfo?
}
