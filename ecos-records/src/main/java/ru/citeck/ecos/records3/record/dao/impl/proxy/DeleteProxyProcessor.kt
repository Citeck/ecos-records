package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records3.record.dao.delete.DelStatus

interface DeleteProxyProcessor : ProxyProcessor {

    fun deletePreProcess(
        recordIds: List<String>,
        context: ProxyProcContext
    )

    /**
     * recordIds and statuses always has same size.
     */
    fun deletePostProcess(
        recordIds: List<String>,
        statuses: List<DelStatus>,
        context: ProxyProcContext
    )
}
