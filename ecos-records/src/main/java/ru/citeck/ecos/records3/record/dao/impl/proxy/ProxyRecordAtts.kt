package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records3.record.atts.dto.RecordAtts

data class ProxyRecordAtts(
    val atts: RecordAtts,
    val additionalAtts: Map<String, Any?> = emptyMap()
)
