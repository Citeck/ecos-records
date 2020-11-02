package ru.citeck.ecos.records3.utils

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.request.error.RecordsError
import ru.citeck.ecos.records2.request.result.DebugResult
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records2.request.query.RecordsQuery as RecordsQueryV0

object V1ConvUtils {

    @JvmStatic
    fun recsQueryV1ToV0(query: RecordsQuery, context: RequestContext): RecordsQueryV0 {
        val v0Query = RecordsQueryV0()
        Json.mapper.applyData(v0Query, query)
        if (context.isMsgEnabled(MsgLevel.DEBUG)) {
            v0Query.isDebug = true
        }
        return v0Query
    }

    @JvmStatic
    fun recsQueryV0ToV1(query: RecordsQueryV0): RecordsQuery {
        val v1Query = RecordsQuery.create()
        Json.mapper.applyData(v1Query, query)
        return v1Query.build()
    }

    @JvmStatic
    fun addErrorMessages(errors: List<RecordsError>?, context: RequestContext) {
        if (errors != null && errors.isNotEmpty()) {
            errors.forEach { error -> context.addMsg(MsgLevel.ERROR) { error } }
        }
    }

    @JvmStatic
    fun addDebugMessage(result: DebugResult, context: RequestContext) {
        if (context.getMessages().isNotEmpty()) {
            val debug: ObjectData = ObjectData.create()
            debug.set("messages", context.getMessages())
            result.debug = debug
        }
    }
}
