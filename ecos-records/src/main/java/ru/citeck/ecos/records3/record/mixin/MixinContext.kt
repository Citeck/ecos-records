package ru.citeck.ecos.records3.record.mixin

interface MixinContext {

    fun getMixin(path: String): MixinAttContext?
}
