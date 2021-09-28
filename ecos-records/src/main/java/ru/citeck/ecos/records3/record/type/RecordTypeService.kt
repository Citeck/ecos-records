package ru.citeck.ecos.records3.record.type

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt

interface RecordTypeService {

    fun getComputedAtts(typeRef: RecordRef): List<RecordComputedAtt>
}
