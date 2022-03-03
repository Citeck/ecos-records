package ru.citeck.ecos.records3.record.request.ctxatts

interface CtxAttsProvider {

    fun fillContextAtts(attributes: MutableMap<String, Any?>)

    fun getOrder(): Float = 0f
}
