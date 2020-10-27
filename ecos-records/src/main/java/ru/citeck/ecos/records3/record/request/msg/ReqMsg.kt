package ru.citeck.ecos.records3.record.request.msg

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.DataValue
import java.time.Instant

@JsonDeserialize(builder = ReqMsg.Builder::class)
data class ReqMsg(
    val level: MsgLevel,
    val time: Instant,
    val type: String,
    val msg: DataValue,
    val queryTrace: List<String>
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

        var level: MsgLevel = MsgLevel.INFO
            private set
        var time: Instant = Instant.now()
            private set
        lateinit var type: String
            private set
        lateinit var msg: DataValue
            private set
        var queryTrace: List<String> = emptyList()
            private set

        constructor(base: ReqMsg) : this() {
            this.level = base.level
            this.time = base.time
            this.type = base.type
            this.msg = base.msg.copy()
            this.queryTrace = ArrayList(base.queryTrace)
        }

        fun withLevel(level: MsgLevel): Builder {
            this.level = level;
            return this;
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

        fun withQueryTrace(queryTrace: List<String>): Builder {
            this.queryTrace = queryTrace
            return this
        }

        fun build(): ReqMsg {
            return ReqMsg(level, time, type, msg, queryTrace)
        }
    }
}
