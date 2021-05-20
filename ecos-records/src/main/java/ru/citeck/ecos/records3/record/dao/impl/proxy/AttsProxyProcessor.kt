package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

interface AttsProxyProcessor : ProxyProcessor {

    fun prepareAttsSchema(schemaAtts: List<SchemaAtt>): List<SchemaAtt>

    fun postProcessAtts(atts: List<ProxyRecordAtts>): List<ProxyRecordAtts>
}
