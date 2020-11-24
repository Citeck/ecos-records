package ru.citeck.ecos.records3.record.type

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.service.computed.ComputedAtt

class DefaultRecTypeService : RecordTypeService {

    override fun getComputedAtts(typeRef: RecordRef): List<ComputedAtt> {
        return emptyList()
    }
}
