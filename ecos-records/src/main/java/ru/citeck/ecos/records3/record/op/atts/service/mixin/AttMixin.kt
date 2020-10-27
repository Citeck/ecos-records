package ru.citeck.ecos.records3.record.op.atts.service.mixin

import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttValueCtx

interface AttMixin {

    @Throws(Exception::class)
    fun getAtt(path: String, value: AttValueCtx): Any?

    fun getProvidedAtts() : Collection<String>
}
