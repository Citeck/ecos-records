package ru.citeck.ecos.records3.record.request

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize as JackJsonDeserialize

@JsonDeserialize(builder = RequestCtxData.Builder::class)
@JackJsonDeserialize(builder = RequestCtxData.Builder::class)
data class RequestCtxData(
    val requestId: String,
    val requestTrace: List<String>,
    val locale: Locale,
    val ctxAtts: Map<String, Any?>,
    val computedAttsDisabled: Boolean,
    val msgLevel: MsgLevel,
    val omitErrors: Boolean,
    val readOnly: Boolean,
    val txnId: UUID?,
    val txnOwner: Boolean,
    val sourceIdMapping: Map<String, String>
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
        var msgLevel: MsgLevel = MsgLevel.WARN
        var omitErrors: Boolean = false
        var readOnly: Boolean = false
        var txnId: UUID? = null
        var txnOwner: Boolean = false
        var sourceIdMapping: Map<String, String> = emptyMap()

        constructor(base: RequestCtxData) : this() {
            requestId = base.requestId
            requestTrace = base.requestTrace.toMutableList()
            locale = base.locale
            ctxAtts = base.ctxAtts.toMutableMap()
            computedAttsDisabled = base.computedAttsDisabled
            msgLevel = base.msgLevel
            omitErrors = base.omitErrors
            readOnly = base.readOnly
            txnId = base.txnId
            txnOwner = base.txnOwner
            sourceIdMapping = base.sourceIdMapping
        }

        fun withOmitErrors(omitErrors: Boolean): Builder {
            this.omitErrors = omitErrors
            return this
        }

        fun withRequestId(requestId: String): Builder {
            this.requestId = requestId
            return this
        }

        fun withRequestTrace(requestTrace: List<String>): Builder {
            this.requestTrace = requestTrace.toMutableList()
            return this
        }

        fun withLocale(locale: Locale): Builder {
            this.locale = locale
            return this
        }

        fun withCtxAtts(contextAtts: Map<String, Any?>): Builder {
            this.ctxAtts = contextAtts.toMutableMap()
            return this
        }

        fun withComputedAttsDisabled(disabled: Boolean): Builder {
            this.computedAttsDisabled = disabled
            return this
        }

        fun withMsgLevel(level: MsgLevel): Builder {
            this.msgLevel = level
            return this
        }

        fun withReadOnly(readOnly: Boolean?): Builder {
            this.readOnly = readOnly ?: false
            return this
        }

        fun withTxnId(txnId: UUID?): Builder {
            this.txnId = txnId
            return this
        }

        fun withTxnOwner(txnOwner: Boolean?): Builder {
            this.txnOwner = txnOwner ?: false
            return this
        }

        fun withSourceIdMapping(sourceIdMapping: Map<String, String>?): Builder {
            this.sourceIdMapping = sourceIdMapping ?: emptyMap()
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
                msgLevel,
                omitErrors,
                readOnly,
                txnId,
                txnOwner,
                sourceIdMapping
            )
        }
    }
}
