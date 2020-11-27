package ru.citeck.ecos.records3.record.type

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.service.computed.ComputedAtt

interface RecordTypeService {

    fun getComputedAtts(typeRef: RecordRef): List<ComputedAtt>

    // fun getNumTemplate(typeRef: RecordRef): RecordRef?
}
