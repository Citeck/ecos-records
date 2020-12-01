package ru.citeck.ecos.records3.record.request.context

import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.function.Supplier

object SystemContextUtil {

    private const val SYSTEM_CONTEXT_KEY = "is-system-context"

    @JvmStatic
    @JvmOverloads
    fun <T : Any> doAsSystemIfNotSystemContextJ(
        action: Supplier<T>,
        systemCtxValue: T,
        context: RequestContext = RequestContext.getCurrentNotNull()
    ): T {
        return doAsSystemIfNotSystemContext({ action.get() }, systemCtxValue, context)
    }

    fun <T : Any> doAsSystemIfNotSystemContext(
        action: () -> T,
        systemCtxValue: T,
        context: RequestContext = RequestContext.getCurrentNotNull()
    ): T {
        if (isSystemContext(context)) {
            return systemCtxValue
        }
        return doAsSystemNotNull(action)
    }

    fun <T : Any> doAsSystem(
        action: () -> T?,
        context: RequestContext = RequestContext.getCurrentNotNull()
    ): T? {
        return context.doWithVar(SYSTEM_CONTEXT_KEY, true, action)
    }

    fun <T : Any> doAsSystemNotNull(
        action: () -> T,
        context: RequestContext = RequestContext.getCurrentNotNull()
    ): T {
        return context.doWithVarNotNull(SYSTEM_CONTEXT_KEY, true, action)
    }

    @JvmStatic
    @JvmOverloads
    fun <T : Any> doAsSystemJ(
        action: Supplier<T?>,
        context: RequestContext = RequestContext.getCurrentNotNull()
    ): T? {
        return doAsSystem({ action.get() }, context)
    }

    @JvmStatic
    @JvmOverloads
    fun <T : Any> doAsSystemNotNullJ(
        action: Supplier<T>,
        context: RequestContext = RequestContext.getCurrentNotNull()
    ): T {
        return doAsSystemNotNull({ action.get() }, context)
    }

    @JvmStatic
    @JvmOverloads
    fun isSystemContext(context: RequestContext = RequestContext.getCurrentNotNull()): Boolean {
        return context.getVar<Boolean>(SYSTEM_CONTEXT_KEY) == true
    }
}
