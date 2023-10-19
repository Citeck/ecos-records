package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.webapp.api.entity.EntityRef

interface MutateProxyProcessor : ProxyProcessor {

    fun mutatePreProcess(atts: List<LocalRecordAtts>, context: ProxyProcContext): List<LocalRecordAtts>

    fun mutatePostProcess(records: List<EntityRef>, context: ProxyProcContext): List<EntityRef>
}
