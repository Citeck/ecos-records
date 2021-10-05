package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records3.record.dao.delete.DelStatus

interface DeleteProxyProcessor : ProxyProcessor {

    fun deletePreProcess(
        recordsId: List<String>,
        context: ProxyProcContext
    )

    /**
     * recordsId and status always has same size.
     */
    fun deletePostProcess(
        recordsId: List<String>,
        statuses: List<DelStatus>,
        context: ProxyProcContext
    )
}
