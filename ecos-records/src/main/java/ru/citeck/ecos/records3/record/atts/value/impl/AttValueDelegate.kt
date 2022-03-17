package ru.citeck.ecos.records3.record.atts.value.impl

import ru.citeck.ecos.records3.record.atts.value.AttValue

open class AttValueDelegate(val impl: AttValue) : AttValue {

    override fun init() = impl.init()
    override fun getId() = impl.id
    override fun getDisplayName() = impl.displayName
    override fun asText() = impl.asText()
    override fun getAs(type: String) = impl.getAs(type)
    override fun asDouble() = impl.asDouble()
    override fun asBoolean() = impl.asBoolean()
    override fun asJson() = impl.asJson()
    override fun asRaw() = impl.asRaw()
    override fun has(name: String) = impl.has(name)
    override fun getAtt(name: String) = impl.getAtt(name)
    override fun getEdge(name: String) = impl.getEdge(name)
    override fun getType() = impl.type
}
