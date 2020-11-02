package ru.citeck.ecos.records3.record.op.atts.dto

import lombok.extern.slf4j.Slf4j
import ru.citeck.ecos.commons.data.ObjectData

@Slf4j
data class LocalRecordAtts(
    val id: String = "",
    val attributes: ObjectData = ObjectData.create()
) {

    constructor(atts: RecordAtts) : this(atts.getId().id, atts.getAtts())
}
