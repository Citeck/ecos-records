package ru.citeck.ecos.records3.rest.v1

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*

abstract class RequestBody() {

    var requestId = ""
    var msgLevel: MsgLevel = MsgLevel.WARN

    private var requestTrace: List<String> = emptyList()

    constructor(other: RequestBody) : this() {
        this.requestId = other.requestId
        this.msgLevel = other.msgLevel
        this.requestTrace = other.requestTrace
    }

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
