package ru.citeck.ecos.records3.record.request

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.request.error.RecordsError
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.request.msg.MsgType
import ru.citeck.ecos.records3.record.request.msg.ReqMsg
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class RequestContext {

    companion object {

        private val log = KotlinLogging.logger {}

        private var defaultServices: RecordsServiceFactory? = null
        private val current: ThreadLocal<RequestContext> = ThreadLocal()

        fun setDefaultServicesIfNotSet(defaultServices: RecordsServiceFactory) {
            if (this.defaultServices == null) {
                this.defaultServices = defaultServices
            }
        }

        fun setDefaultServices(defaultServices: RecordsServiceFactory) {
            this.defaultServices = defaultServices
        }

        @JvmStatic
        fun getCurrent(): RequestContext? {
            return current.get()
        }

        @JvmStatic
        fun getCurrentNotNull(): RequestContext {
            return getCurrent() ?: error(
                "Request context is mandatory. " +
                    "Add RequestContex.withCtx(ctx -> {...}) before call"
            )
        }

        @JvmStatic
        fun getLocale(): Locale {
            return getCurrent()?.getCtxLocale() ?: Locale.ENGLISH
        }

        @JvmStatic
        fun <T> doWithAtts(atts: Map<String, Any?>, action: (RequestContext) -> T): T {
            return doWithCtx(null, { it.withCtxAtts(atts) }, action)
        }

        fun <T> doWithAtts(atts: Map<String, Any?>, action: () -> T): T {
            return doWithCtx(null, { it.withCtxAtts(atts) }) { action.invoke() }
        }

        @JvmStatic
        fun <T> doWithAttsJ(atts: Map<String, Any?>, action: Supplier<T>): T {
            return doWithCtx(null, { it.withCtxAtts(atts) }) { action.get() }
        }

        @JvmStatic
        fun <T> doWithAttsJ(atts: Map<String, Any?>, action: Runnable) {
            return doWithCtx(null, { it.withCtxAtts(atts) }) { action.run() }
        }

        @JvmStatic
        fun <T> doWithCtx(action: (RequestContext) -> T): T {
            return doWithCtx(null, null, action)
        }

        @JvmStatic
        fun <T> doWithCtx(factory: RecordsServiceFactory?, action: (RequestContext) -> T): T {
            return doWithCtx(factory, null, action)
        }

        fun <T> doWithCtx(ctxData: ((RequestCtxData.Builder) -> Unit)?, action: (RequestContext) -> T): T {
            return doWithCtx(null, ctxData, action)
        }

        @JvmStatic
        fun <T> doWithCtxJ(action: (RequestContext) -> T): T {
            return doWithCtx(action)
        }

        @JvmStatic
        fun <T> doWithCtxJ(factory: RecordsServiceFactory?, action: (RequestContext) -> T): T {
            return doWithCtx(factory, action)
        }

        @JvmStatic
        fun <T> doWithCtxJ(ctxData: Consumer<RequestCtxData.Builder>, action: (RequestContext) -> T): T {
            return doWithCtx(null, { ctxData.accept(it) }, action)
        }

        @JvmStatic
        fun <T> doWithCtxJ(
            factory: RecordsServiceFactory?,
            ctxData: Consumer<RequestCtxData.Builder>,
            action: (RequestContext) -> T
        ): T {

            return doWithCtx(factory, { b: RequestCtxData.Builder -> ctxData.accept(b) }, action)
        }

        fun <T> doWithCtx(
            factory: RecordsServiceFactory?,
            ctxData: ((RequestCtxData.Builder) -> Unit)?,
            action: (RequestContext) -> T
        ): T {

            var current = getCurrent()
            val notNullServices = factory ?: current?.serviceFactory ?: defaultServices
                ?: error("RecordsServiceFactory is not found in context!")

            var isContextOwner = false

            if (current == null) {

                val builder: RequestCtxData.Builder = RequestCtxData.create()
                ctxData?.invoke(builder)
                current = RequestContext()

                val contextAtts = HashMap(notNullServices.defaultCtxAttsProvider.getContextAttributes())
                contextAtts["now"] = Date()

                current.ctxData = builder.withCtxAtts(contextAtts)
                    .withLocale(notNullServices.localeSupplier.invoke())
                    .build()
                current.serviceFactory = notNullServices

                RequestContext.current.set(current)
                isContextOwner = true
            }

            val prevCtxData = current.ctxData
            if (ctxData != null) {

                val builder = prevCtxData.copy()
                ctxData.invoke(builder)

                val ctxAtts = HashMap(prevCtxData.ctxAtts)
                ctxAtts.putAll(builder.ctxAtts)
                builder.ctxAtts = ctxAtts

                current.ctxData = builder.build()
            }

            val currentMessages: MutableList<ReqMsg> = current.messages
            current.messages = ArrayList()

            return try {
                action.invoke(current)
            } finally {
                currentMessages.addAll(current.messages)
                current.messages = currentMessages
                current.ctxData = prevCtxData
                if (isContextOwner) {
                    current.messages.forEach { msg ->
                        when (msg.level) {
                            MsgLevel.ERROR -> log.error(msg.toString())
                            MsgLevel.WARN -> log.warn(msg.toString())
                            MsgLevel.DEBUG -> log.debug(msg.toString())
                        }
                    }
                    RequestContext.current.remove()
                }
            }
        }
    }

    private val msgTypeByClass: MutableMap<Class<*>, String> = ConcurrentHashMap<Class<*>, String>()
    private val ctxVars: MutableMap<String, Any> = ConcurrentHashMap<String, Any>()

    lateinit var ctxData: RequestCtxData

    private lateinit var serviceFactory: RecordsServiceFactory

    private var messages: MutableList<ReqMsg> = ArrayList()

    fun <T : Any> doWithVar(key: String, data: Any?, action: () -> T?): T? {
        val prevValue = getVar<Any>(key)
        putVar(key, data)
        return try {
            action.invoke()
        } finally {
            putVar(key, prevValue)
        }
    }

    fun <T : Any> doWithVarNotNull(key: String, data: Any?, action: () -> T): T {
        val prevValue = getVar<Any>(key)
        putVar(key, data)
        return try {
            action.invoke()
        } finally {
            putVar(key, prevValue)
        }
    }

    fun getCtxLocale(): Locale {
        return ctxData.locale
    }

    fun hasVar(key: String): Boolean {
        return ctxVars.containsKey(key)
    }

    fun putVar(key: String, data: Any?) {
        if (data != null) {
            ctxVars[key] = data
        } else {
            removeVar<Any>(key)
        }
    }

    fun <T : Any> getVar(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return ctxVars[key] as? T?
    }

    fun <T : Any> removeVar(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return ctxVars.remove(key) as? T?
    }

    fun <T : Any> getOrPutVar(key: String, type: Class<*>, newValue: () -> T): T {

        var value = ctxVars.computeIfAbsent(key) { newValue.invoke() }
        if (!type.isInstance(value)) {
            log.warn(
                "Context data with the key '" + key + "' is not an instance of a " + type + ". " +
                    "Data will be overridden. Current data: " + value
            )
            value = newValue.invoke()
            ctxVars[key] = value
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    fun <K, V> getMap(key: String): MutableMap<K, V> {
        return getOrPutVar(key, MutableMap::class.java) { LinkedHashMap() }
    }

    fun <T> getList(key: String): MutableList<T> {
        return getOrPutVar(key, MutableList::class.java) { ArrayList() }
    }

    fun <T> getSet(key: String): MutableSet<T>? {
        return getOrPutVar(key, MutableSet::class.java) { HashSet() }
    }

    fun getCount(key: String): Int {
        return getOrPutVar(key, AtomicInteger::class.java) { AtomicInteger() }.get()
    }

    fun incrementCount(key: String): Int {
        return getOrPutVar(key, AtomicInteger::class.java) { AtomicInteger() }.incrementAndGet()
    }

    @JvmOverloads
    fun decrementCount(key: String, allowNegative: Boolean = true): Int {
        val counter: AtomicInteger = getOrPutVar(key, AtomicInteger::class.java) { AtomicInteger() }
        return if (allowNegative || counter.get() > 0) {
            counter.decrementAndGet()
        } else {
            counter.get()
        }
    }

    fun getServices(): RecordsServiceFactory {
        return serviceFactory
    }

    fun getCtxAtts(): Map<String, Any?> {
        return ctxData.ctxAtts
    }

    fun clearMessages() {
        messages.clear()
    }

    fun getMessages(): List<ReqMsg> {
        return messages
    }

    fun isMsgEnabled(level: MsgLevel): Boolean {
        return ctxData.msgLevel.isEnabled(level)
    }

    fun addAllMsgs(messages: Iterable<ReqMsg>) {
        messages.forEach(this::addMsg)
    }

    fun addMsg(msg: ReqMsg) {
        messages.add(msg)
    }

    fun addMsg(level: MsgLevel, msg: String) {
        addMsg(level) { msg }
    }

    fun addMsgJ(level: MsgLevel, msg: Supplier<Any?>) {
        addMsg(level) { msg.get() }
    }

    fun addMsg(level: MsgLevel, msg: () -> Any?) {
        if (!isMsgEnabled(level)) {
            return
        }
        var msgValue = msg.invoke()
        if (msgValue == null) {
            msgValue = "null"
        }
        val type = if (msgValue is String) {
            "text"
        } else {
            getMessageTypeByClass(msgValue.javaClass)
        }
        messages.add(
            ReqMsg(
                level,
                Instant.now(),
                type,
                DataValue.create(msgValue),
                ctxData.requestId,
                ctxData.requestTrace
            )
        )
    }

    private fun getMessageTypeByClass(clazz: Class<*>): String {
        return if (clazz == String::class.java) {
            "text"
        } else {
            msgTypeByClass.computeIfAbsent(clazz) { c ->
                c.getAnnotation(MsgType::class.java)?.value ?: "any"
            }
        }
    }

    fun getRecordErrors(): List<RecordsError> {
        return messages
            .filter { MsgLevel.ERROR.isEnabled(it.level) }
            .filter { RecordsError.MSG_TYPE == it.type }
            .mapNotNull { Json.mapper.convert(it.msg, RecordsError::class.java) }
    }

    fun getErrors(): List<ReqMsg> {
        return messages.filter { MsgLevel.ERROR.isEnabled(it.level) }
    }
}
