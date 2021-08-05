package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts

interface MutateProxyProcessor : ProxyProcessor {

    fun mutatePreProcess(atts: List<LocalRecordAtts>): List<LocalRecordAtts>

    fun mutatePostProcess(records: List<RecordRef>): List<RecordRef>
}
