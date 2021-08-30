package ru.citeck.ecos.records3.record.mixin

object EmptyMixinContext : MixinContext {
    override fun getMixin(path: String): MixinAttContext? = null
}
