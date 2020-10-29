package ru.citeck.ecos.records3.record.request

import com.fasterxml.jackson.annotation.JsonSetter as JackJsonSetter
import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*

@JsonDeserialize(builder = RequestCtxData.Builder::class)
@JackJsonDeserialize(builder = RequestCtxData.Builder::class)
data class RequestCtxData(
    val requestId: String,
    val requestTrace: List<String>,
    val locale: Locale,
    val ctxAtts: Map<String, Any?>,
    val computedAttsDisabled: Boolean,
    val msgLevel: MsgLevel
) {

    companion object {

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): RequestCtxData {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): RequestCtxData {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var requestId: String = ""
        var requestTrace = mutableListOf<String>()
        var locale: Locale = Locale.ENGLISH
        var ctxAtts: MutableMap<String, Any?> = mutableMapOf()
        var computedAttsDisabled = false
        var msgLevel: MsgLevel = MsgLevel.INFO

        constructor(base: RequestCtxData) : this() {
            requestId = base.requestId
            requestTrace = base.requestTrace.toMutableList()
            locale = base.locale
            ctxAtts = base.ctxAtts.toMutableMap()
            computedAttsDisabled = base.computedAttsDisabled
            msgLevel = base.msgLevel
        }

        fun withRequestId(requestId: String) : Builder {
            this.requestId = requestId;
            return this
        }

        fun withRequestTrace(requestTrace: List<String>) : Builder {
            this.requestTrace = requestTrace.toMutableList()
            return this
        }

        fun withLocale(locale: Locale) : Builder {
            this.locale = locale
            return this
        }

        fun withCtxAtts(contextAtts: Map<String, Any?>) : Builder {
            this.ctxAtts = contextAtts.toMutableMap()
            return this
        }

        fun withComputedAttsDisabled(disabled: Boolean) : Builder {
            this.computedAttsDisabled = disabled
            return this
        }

        fun withMsgLevel(level: MsgLevel) : Builder {
            this.msgLevel = level
            return this
        }

        fun build(): RequestCtxData {

            if (requestId.isBlank()) {
                requestId = UUID.randomUUID().toString()
            }

            return RequestCtxData(
                requestId,
                requestTrace,
                locale,
                ctxAtts,
                computedAttsDisabled,
                msgLevel
            )
        }
    }
}
