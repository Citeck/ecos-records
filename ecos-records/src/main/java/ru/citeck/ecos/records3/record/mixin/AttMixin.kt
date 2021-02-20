package ru.citeck.ecos.records3.record.mixin

import ru.citeck.ecos.records3.record.atts.value.AttValueCtx

interface AttMixin {

    @Throws(Exception::class)
    fun getAtt(path: String, value: AttValueCtx): Any?

    fun getProvidedAtts(): Collection<String>
}
