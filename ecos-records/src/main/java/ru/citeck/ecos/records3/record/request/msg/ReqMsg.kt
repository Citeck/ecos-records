package ru.citeck.ecos.records3.record.request.msg

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.DataValue
import java.time.Instant
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@JsonDeserialize(builder = ReqMsg.Builder::class)
@JackJsonDeserialize(builder = ReqMsg.Builder::class)
data class ReqMsg(
    val level: MsgLevel,
    val time: Instant,
    val type: String,
    val msg: DataValue,
    val requestId: String,
    val requestTrace: List<String>
) {
    companion object {

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): ReqMsg {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): ReqMsg {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var level: MsgLevel = MsgLevel.DEBUG
        var time: Instant = Instant.now()
        lateinit var type: String
        lateinit var msg: DataValue
        var requestId: String = ""
        var requestTrace: List<String> = emptyList()

        constructor(base: ReqMsg) : this() {
            this.level = base.level
            this.time = base.time
            this.type = base.type
            this.msg = base.msg.copy()
            this.requestId = base.requestId
            this.requestTrace = ArrayList(base.requestTrace)
        }

        fun withRequestId(requestId: String): Builder {
            this.requestId = requestId
            return this
        }

        fun withLevel(level: MsgLevel): Builder {
            this.level = level
            return this
        }

        fun withTime(time: Instant): Builder {
            this.time = time
            return this
        }

        fun withType(type: String): Builder {
            this.type = type
            return this
        }

        fun withMsg(msg: DataValue): Builder {
            this.msg = msg
            return this
        }

        fun withQueryTrace(requestTrace: List<String>): Builder {
            this.requestTrace = requestTrace
            return this
        }

        fun build(): ReqMsg {
            return ReqMsg(level, time, type, msg, requestId, requestTrace)
        }
    }
}
