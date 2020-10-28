package ru.citeck.ecos.records3.rest.v1

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*

abstract class RequestBody {

    var requestId = ""
    var requestTrace: MutableList<String> = ArrayList()
        private set
    var msgLevel: MsgLevel = MsgLevel.INFO

    fun setRequestTrace(requestTrace: List<String>?) {
        if (requestTrace != null) {
            this.requestTrace = ArrayList(requestTrace)
        } else {
            this.requestTrace = ArrayList()
        }
    }

    val version: Int
        get() = 1

    override fun toString(): String {
        return Json.mapper.toString(this) ?: "RequestBody"
    }
}
