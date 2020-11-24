package ru.citeck.ecos.records3.record.op.atts.service.computed.script

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValueCtx

class AttValueScriptCtxImpl(val impl: AttValueCtx) : AttValueScriptCtx {

    override fun getId(): String {
        return getRef().toString()
    }

    override fun getRef(): RecordRef {
        return impl.getRef()
    }

    override fun getLocalId(): String {
        return impl.getLocalId()
    }

    override fun load(attributes: Any?): DataValue {

        val atts = ComputedScriptUtils.toRecordAttsMap(attributes) ?: return DataValue.NULL

        val resolvedAtts = impl.getAtts(atts.first).getData()
        return if (atts.second) {
            resolvedAtts.firstOrNull() ?: DataValue.NULL
        } else {
            resolvedAtts
        }
    }
}
