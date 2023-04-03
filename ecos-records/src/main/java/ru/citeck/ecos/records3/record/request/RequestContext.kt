package ru.citeck.ecos.records3.record.request

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.func.UncheckedRunnable
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.records2.RecordRef
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
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class RequestContext {

    companion object {

        private const val TXN_MUT_RECORDS_KEY = "__txn_mut_records_key__"
        private const val TXN_OWNED_KEY = "__txn_owned__"
        private const val READ_ONLY_CACHE_KEY = "__read_only_cache__"
        private const val AFTER_COMMIT_ACTIONS_KEY = "__after_commit_actions__"
        private const val BEFORE_COMMIT_ACTIONS_KEY = "__before_commit_actions__"

        private const val BEFORE_COMMIT_ACTIONS_MAX_ITERATIONS = 20
        private const val AFTER_COMMIT_ACTIONS_MAX_ITERATIONS = 10

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
        @Deprecated(
            "Use I8nContext.getLocale() instead",
            ReplaceWith("I8nContext.getLocale()", "ru.citeck.ecos.context.lib.i18n.I8nContext")
        )
        fun getLocale(): Locale {
            return I18nContext.getLocale()
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
        fun <T> doWithTxnJ(readOnly: Boolean, requiresNew: Boolean, action: UncheckedSupplier<T>): T {
            return doWithTxn(readOnly, requiresNew) { action.get() }
        }

        @JvmStatic
        @JvmOverloads
        fun doWithTxnJ(readOnly: Boolean = false, requiresNew: Boolean = false, action: Runnable) {
            doWithTxn(readOnly, requiresNew) { action.run() }
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
        fun <T> doWithTxn(readOnly: Boolean = false, requiresNew: Boolean = false, action: () -> T): T {
            var isTxnOwner = false
            return doWithCtx(
                null,
                {
                    if (requiresNew || it.txnId == null) {
                        isTxnOwner = true
                        it.withTxnId(UUID.randomUUID())
                        it.withTxnOwner(true)
                    }
                    it.withReadOnly(readOnly)
                }
            ) {
                if (isTxnOwner && !readOnly) {
                    it.putVar(TXN_OWNED_KEY, true)
                    try {
                        val res = action.invoke()
                        it.completeTxn(true)
                        res
                    } catch (originalEx: Throwable) {
                        try {
                            it.completeTxn(false)
                        } catch (e: Throwable) {
                            log.error("Transaction rollback completed with error", e)
                            originalEx.addSuppressed(e)
                        }
                        throw originalEx
                    } finally {
                        it.removeVar<Any>(TXN_OWNED_KEY)
                    }
                } else {
                    action.invoke()
                }
            }
        }

        fun doAfterCommit(action: () -> Unit) {
            getCurrentNotNull().doAfterCommit(action)
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
                            MsgLevel.ERROR -> log.error(msg.toString())
                            MsgLevel.WARN -> log.warn(msg.toString())
                            MsgLevel.DEBUG -> log.debug(msg.toString())
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

    private fun completeTxn(success: Boolean) {
        val txnId = ctxData.txnId
        if (txnId == null || ctxData.readOnly) {
            return
        }
        val mutRecords = getMap<UUID, Set<RecordRef>>(TXN_MUT_RECORDS_KEY)
        val txnRecords = mutRecords[txnId] ?: emptySet()

        val beforeCommitActions = getList<OrderedAction>(BEFORE_COMMIT_ACTIONS_KEY)
        val afterCommitActions = getList<OrderedAction>(AFTER_COMMIT_ACTIONS_KEY)

        try {
            if (success) {
                invokeActionsList(false, beforeCommitActions, BEFORE_COMMIT_ACTIONS_MAX_ITERATIONS) {
                    error(
                        "Before commit actions iterations limit was exceeded: " +
                            "$BEFORE_COMMIT_ACTIONS_MAX_ITERATIONS. It may be cyclic dependency."
                    )
                }
            }
            if (txnRecords.isNotEmpty()) {
                if (success) {
                    serviceFactory.recordsResolver.commit(txnRecords.toList())
                } else {
                    serviceFactory.recordsResolver.rollback(txnRecords.toList())
                }
            }
            if (success) {
                AuthContext.runAsSystem {
                    invokeActionsList(true, afterCommitActions, AFTER_COMMIT_ACTIONS_MAX_ITERATIONS) {
                        log.warn {
                            "After commit actions iterations limit was exceeded: " +
                                "$AFTER_COMMIT_ACTIONS_MAX_ITERATIONS. It may be cyclic dependency."
                        }
                    }
                }
            }
            mutRecords.remove(txnId)
        } finally {
            beforeCommitActions.clear()
            afterCommitActions.clear()
        }
    }

    private fun invokeActionsList(
        allowErrors: Boolean,
        actionsList: MutableList<OrderedAction>,
        maxIterations: Int,
        onMaxIterations: () -> Unit
    ) {
        if (actionsList.isEmpty()) {
            return
        }
        try {
            var iterations = maxIterations
            while (--iterations > 0 && actionsList.isNotEmpty()) {
                val actionsToExecute = ArrayList(actionsList)
                actionsList.clear()
                actionsToExecute.sortBy { it.order }

                for (action in actionsToExecute) {
                    if (allowErrors) {
                        try {
                            action.action.invoke()
                        } catch (e: Throwable) {
                            log.error("Error while action execution", e)
                        }
                    } else {
                        action.action.invoke()
                    }
                }
            }
            if (iterations == 0) {
                onMaxIterations.invoke()
            }
        } finally {
            actionsList.clear()
        }
    }

    fun doAfterCommit(action: () -> Unit) {
        doAfterCommit(0f, action)
    }

    fun doAfterCommit(order: Float, action: () -> Unit) {
        val txnId = ctxData.txnId
        if (txnId == null) {
            action.invoke()
        } else {
            getList<OrderedAction>(AFTER_COMMIT_ACTIONS_KEY).add(OrderedAction(order, action))
        }
    }

    fun doBeforeCommit(action: () -> Unit) {
        doBeforeCommit(0f, action)
    }

    fun doBeforeCommit(order: Float, action: () -> Unit) {
        val txnId = ctxData.txnId
        if (txnId == null) {
            action.invoke()
        } else {
            getList<OrderedAction>(BEFORE_COMMIT_ACTIONS_KEY).add(OrderedAction(order, action))
        }
    }

    fun getTxnChangedRecords(): MutableSet<RecordRef>? {
        val txnId = ctxData.txnId ?: return null
        return getMap<UUID, MutableSet<RecordRef>>(TXN_MUT_RECORDS_KEY).computeIfAbsent(txnId) { LinkedHashSet() }
    }

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

    @Deprecated(
        "Use I18nContext.getLocale() instead",
        ReplaceWith("I18nContext.getLocale()", "ru.citeck.ecos.context.lib.i18n.I18nContext")
    )
    fun getCtxLocale(): Locale {
        return I18nContext.getLocale()
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

    private class OrderedAction(
        val order: Float,
        val action: () -> Unit
    )
}
