package ru.citeck.ecos.records3.record.atts.computed.script

import ru.citeck.ecos.webapp.api.entity.EntityRef

interface AttValueScriptCtx {

    fun getId(): String

    fun getRef(): EntityRef

    fun getLocalId(): String

    fun load(attributes: Any?): Any?

    fun save(): AttValueScriptCtx

    fun att(attribute: String, value: Any?)

    fun reset()
}
