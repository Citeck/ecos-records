package ru.citeck.ecos.records3.record.op.atts.service.computed.script

import ru.citeck.ecos.records2.RecordRef

interface AttValueScriptCtx {

    fun getId(): String

    fun getRef(): RecordRef

    fun getLocalId(): String

    fun load(attributes: Any?): Any?
}
