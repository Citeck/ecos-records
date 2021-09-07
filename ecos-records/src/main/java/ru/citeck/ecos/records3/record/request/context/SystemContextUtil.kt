package ru.citeck.ecos.records3.record.request.context

import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.function.Supplier

@Deprecated("Use AuthContext instead")
object SystemContextUtil {

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
        return AuthContext.runAsSystem(action)
    }

    fun <T : Any> doAsSystemNotNull(
        action: () -> T,
        context: RequestContext = RequestContext.getCurrentNotNull()
    ): T {
        return AuthContext.runAsSystem(action)
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
    fun isSystemContext(context: RequestContext? = RequestContext.getCurrent()): Boolean {
        return AuthContext.isRunAsSystem()
    }
}
