package ru.citeck.ecos.records3.rest.v1

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*

abstract class RequestBody {

    var requestId = ""
    var msgLevel: MsgLevel = MsgLevel.INFO

    private var requestTrace: List<String> = emptyList()

    fun getRequestTrace(): List<String> {
        return requestTrace
    }

    fun setRequestTrace(requestTrace: List<String>?) {
        if (requestTrace != null) {
            this.requestTrace = ArrayList(requestTrace)
        } else {
            this.requestTrace = ArrayList()
        }
    }

    fun getVersion() = 1

    override fun toString() = Json.mapper.toString(this) ?: "RequestBody"
}
