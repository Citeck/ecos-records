package ru.citeck.ecos.records3.record.atts.computed

import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.function.Supplier

object ComputedUtils {

    private const val IS_NEW_RECORD_CTX_KEY = "computed-is-new-record"
    private const val IS_RECORD_MUTATED = "computed-is-record-mutated"

    fun <T : Any> doWithNewRecord(action: () -> T?): T? {
        return RequestContext.getCurrentNotNull().doWithVar(IS_NEW_RECORD_CTX_KEY, true) {
            doWithMutatedRecord(action)
        }
    }

    @JvmStatic
    fun <T : Any> doWithNewRecordJ(action: Supplier<T?>): T? {
        return doWithNewRecord { action.get() }
    }

    fun <T : Any> doWithMutatedRecord(action: () -> T?): T? {
        return RequestContext.getCurrentNotNull().doWithVar(IS_RECORD_MUTATED, true, action)
    }

    fun <T : Any> doWithMutatedRecordJ(action: Supplier<T?>): T? {
        return doWithMutatedRecord { action.get() }
    }

    fun isNewRecord(): Boolean {
        return RequestContext.getCurrent()?.getVar<Boolean>(IS_NEW_RECORD_CTX_KEY) == true
    }

    fun isMutatedRecord(): Boolean {
        return RequestContext.getCurrent()?.getVar<Boolean>(IS_RECORD_MUTATED) == true
    }
}
