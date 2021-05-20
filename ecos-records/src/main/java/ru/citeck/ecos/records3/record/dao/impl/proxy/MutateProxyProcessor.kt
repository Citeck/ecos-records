package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts

interface MutateProxyProcessor : ProxyProcessor {

    fun prepareMutation(atts: List<LocalRecordAtts>): List<LocalRecordAtts>
}
