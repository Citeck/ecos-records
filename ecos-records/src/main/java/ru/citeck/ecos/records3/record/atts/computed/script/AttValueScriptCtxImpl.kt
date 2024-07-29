package ru.citeck.ecos.records3.record.atts.computed.script

import org.graalvm.polyglot.HostAccess.Export
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.script.ScriptUtils
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.webapp.api.entity.EntityRef

class AttValueScriptCtxImpl(
    val impl: AttValueCtx,
    val recordsService: RecordsService? = null,
    private val scriptImplCreator: ValueScriptContextCreator? = null
) : AttValueScriptCtx {

    private var mutateAtts: ObjectData? = null

    @Export
    override fun getId(): String {
        return getRef().toString()
    }

    @Export
    override fun getRef(): EntityRef {
        return impl.getRef()
    }

    @Export
    override fun getLocalId(): String {
        return impl.getLocalId()
    }

    @Export
    override fun load(attributes: Any?): Any? {

        val atts = ComputedScriptUtils.toRecordAttsMap(attributes)
            ?: return DataValue.NULL

        var resolvedAtts = impl.getAtts(atts.first).getData()
        resolvedAtts = if (atts.second) {
            resolvedAtts.firstOrNull() ?: DataValue.NULL
        } else {
            resolvedAtts
        }
        return ScriptUtils.convertToScript(resolvedAtts)
    }

    @Export
    override fun reset() {
        mutateAtts = null
    }

    @Export
    override fun save(): AttValueScriptCtx {
        checkMutationCompatibility()

        val result: AttValueScriptCtx = if (mutateAtts == null || mutateAtts?.size() == 0) {
            this
        } else {
            scriptImplCreator!!.invoke(recordsService!!.mutate(getRef(), mutateAtts!!))
        }

        reset()
        return result
    }

    @Export
    override fun att(attribute: String, value: Any?) {
        checkMutationCompatibility()

        if (mutateAtts == null) {
            mutateAtts = ObjectData.create()
        }

        val javaValue = ScriptUtils.convertToJava(value)
        mutateAtts?.set(attribute, javaValue)
    }

    private fun checkMutationCompatibility() {
        if (recordsService == null || scriptImplCreator == null) {
            error("Mutation does not support")
        }
    }

    @Export
    override fun toString(): String {
        return "ScriptRecord(${getRef()})"
    }
}
