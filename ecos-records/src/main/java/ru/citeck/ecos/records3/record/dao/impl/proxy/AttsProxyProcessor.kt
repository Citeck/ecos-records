package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records3.record.atts.dto.RecordAtts

interface AttsProxyProcessor : ProxyProcessor {

    fun prepareAtts(atts: MutableMap<String, String>)

    /**
     * Return additional atts
     */
    fun postProcessAtts(atts: List<RecordAtts>): List<Map<String, Any>>?
}
