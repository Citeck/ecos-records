package ru.citeck.ecos.records3.record.request

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.func.UncheckedRunnable
import ru.citeck.ecos.records2.request.error.RecordsError
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.request.msg.MsgType
import ru.citeck.ecos.records3.record.request.msg.ReqMsg
import ru.citeck.ecos.webapp.api.func.UncheckedSupplier
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class RequestContext {

    companion object {

        private const val READ_ONLY_CACHE_KEY = "__read_only_cache__"

        private val log = KotlinLogging.logger {}

        private var lastCreatedServices: RecordsServiceFactory? = null
        private var defaultServices: RecordsServiceFactory? = null
        private val current: ThreadLocal<RequestContext> = ThreadLocal()

        fun setDefaultServices(defaultServices: RecordsServiceFactory?) {
            this.defaultServices = defaultServices
        }

        fun setLastCreatedServices(lastCreatedServices: RecordsServiceFactory?) {
            this.lastCreatedServices = lastCreatedServices
        }

        @JvmStatic
        fun getCurrent(): RequestContext? {
            return current.get()
        }

        @JvmStatic
        fun getCurrentNotNull(): RequestContext {
            return getCurrent() ?: error(
                "Request context is mandatory. " +
                    "Add RequestContext.withCtx(ctx -> {...}) before call"
            )
        }

        @JvmStatic
        fun <T> doWithAtts(atts: Map<String, Any?>, action: (RequestContext) -> T): T {
            return doWithCtx(null, { it.withCtxAtts(atts) }, action)
        }

        fun <T> doWithAtts(atts: Map<String, Any?>, action: () -> T): T {
            return doWithCtx(null, { it.withCtxAtts(atts) }) { action.invoke() }
        }

        @JvmStatic
        fun <T> doWithAttsJ(atts: Map<String, Any?>, action: UncheckedSupplier<T>): T {
            return doWithCtx(null, { it.withCtxAtts(atts) }) { action.get() }
        }

        @JvmStatic
        fun doWithAttsJ(atts: Map<String, Any?>, action: Runnable) {
            return doWithCtx(null, { it.withCtxAtts(atts) }) { action.run() }
        }

        @JvmStatic
        fun <T> doWithReadOnly(action: () -> T): T {
            return doWithCtx(null, { it.withReadOnly(true) }) { action.invoke() }
        }

        @JvmStatic
        fun <T> doWithReadOnlyJ(action: UncheckedSupplier<T>): T {
            return doWithReadOnly { action.get() }
        }

        @JvmStatic
        fun doWithReadOnlyJ(action: UncheckedRunnable) {
            return doWithReadOnly { action.invoke() }
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

        fun <T> doWithoutCtx(action: () -> T): T {
            val ctxBefore = current.get()
            try {
                current.remove()
                return action.invoke()
            } finally {
                if (ctxBefore != null) {
                    current.set(ctxBefore)
                }
            }
        }

        fun <T> doWithCtx(
            factory: RecordsServiceFactory?,
            ctxData: ((RequestCtxData.Builder) -> Unit)?,
            action: (RequestContext) -> T
        ): T {

            var current = getCurrent()
            val notNullServices = factory ?: current?.serviceFactory ?: defaultServices ?: lastCreatedServices
                ?: error("RecordsServiceFactory is not found in context!")

            var isContextOwner = false

            if (current == null) {

                val builder: RequestCtxData.Builder = RequestCtxData.create()
                ctxData?.invoke(builder)
                current = RequestContext()

                val ctxAtts = notNullServices.ctxAttsService.getContextAtts()
                current.ctxData = builder.withCtxAtts(ctxAtts).build()

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

                val mergedSrcIdMapping = HashMap(prevCtxData.sourceIdMapping)
                builder.sourceIdMapping.forEach {
                    if (it.value.isBlank()) {
                        mergedSrcIdMapping.remove(it.key)
                    } else {
                        mergedSrcIdMapping[it.key] = it.value
                    }
                }
                builder.withSourceIdMapping(mergedSrcIdMapping)

                current.ctxData = builder.build()
            }

            val currentMessages: MutableList<ReqMsg> = current.messages
            current.messages = ArrayList()

            return try {
                action.invoke(current)
            } finally {
                currentMessages.addAll(current.messages)
                current.messages = currentMessages
                if (current.ctxData.readOnly && !prevCtxData.readOnly) {
                    current.getSet<String>(READ_ONLY_CACHE_KEY).forEach { current.removeVar<Any>(it) }
                    current.removeVar<Any>(READ_ONLY_CACHE_KEY)
                }
                current.ctxData = prevCtxData

                if (isContextOwner) {
                    current.messages.forEach { msg ->
                        when (msg.level) {
                            MsgLevel.ERROR -> log.error { msg.toString() }
                            MsgLevel.WARN -> log.warn { msg.toString() }
                            MsgLevel.DEBUG -> log.debug { msg.toString() }
                            else -> {}
                        }
                    }
                    RequestContext.current.remove()
                }
            }
        }
    }

    private val msgTypeByClass: MutableMap<Class<*>, String> = ConcurrentHashMap()
    private val ctxVars: MutableMap<String, Any> = ConcurrentHashMap()

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

    fun <K, V> getReadOnlyCache(key: String): MutableMap<K, V> {
        if (!ctxData.readOnly) {
            return HashMap()
        }
        return getOrPutVar(key, MutableMap::class.java) {
            getSet<String>(READ_ONLY_CACHE_KEY).add(key)
            LinkedHashMap()
        }
    }

    fun <K, V> getMap(key: String): MutableMap<K, V> {
        return getOrPutVar(key, MutableMap::class.java) { LinkedHashMap() }
    }

    fun <T> getList(key: String): MutableList<T> {
        return getOrPutVar(key, MutableList::class.java) { ArrayList() }
    }

    fun <T> getSet(key: String): MutableSet<T> {
        return getOrPutVar(key, MutableSet::class.java) { LinkedHashSet() }
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
