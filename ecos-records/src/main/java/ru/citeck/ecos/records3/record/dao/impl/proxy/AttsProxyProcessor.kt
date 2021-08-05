package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

interface AttsProxyProcessor : ProxyProcessor {

    fun attsPreProcess(schemaAtts: List<SchemaAtt>, context: ProxyProcContext): List<SchemaAtt>

    fun attsPostProcess(atts: List<ProxyRecordAtts>, context: ProxyProcContext): List<ProxyRecordAtts>
}
