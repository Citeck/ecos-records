package ru.citeck.ecos.records3.record.request.context

import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.function.Supplier

object SystemContextUtil {

    private const val SYSTEM_CONTEXT_KEY = "is-system-context"

    fun <T: Any> doAsSystem(action: () -> T?) : T? {
        return RequestContext.getCurrentNotNull().doWithVar(SYSTEM_CONTEXT_KEY, true, action)
    }

    fun <T: Any> doAsSystemNotNull(action: () -> T) : T {
        return RequestContext.getCurrentNotNull().doWithVarNotNull(SYSTEM_CONTEXT_KEY, true, action)
    }

    fun <T: Any> doAsSystemJ(action: Supplier<T?>) : T? {
        return doAsSystem { action.get() }
    }

    fun <T: Any> doAsSystemNotNullJ(action: Supplier<T>) : T {
        return doAsSystemNotNull { action.get() }
    }

    fun isSystemContext(): Boolean {
        return RequestContext.getCurrentNotNull().getVar<Boolean>(SYSTEM_CONTEXT_KEY) == true
    }
}
