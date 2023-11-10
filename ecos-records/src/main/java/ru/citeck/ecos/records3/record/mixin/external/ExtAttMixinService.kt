package ru.citeck.ecos.records3.record.mixin.external

interface ExtAttMixinService {

    fun getExtMixinContext(typeId: String): ExtAttMixinContext?

    fun getNonLocalExtMixinContext(typeId: String): ExtAttMixinContext?

    fun getNonLocalAtts(): Map<String, Map<String, ExtAttData>>

    fun watchNonLocalAtts(action: (Map<String, Map<String, ExtAttData>>) -> Unit)

    fun register(mixin: ExtAttMixinConfigurer)
}
