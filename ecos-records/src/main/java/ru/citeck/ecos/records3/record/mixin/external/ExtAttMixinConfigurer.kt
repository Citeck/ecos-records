package ru.citeck.ecos.records3.record.mixin.external

interface ExtAttMixinConfigurer {

    @Throws(Exception::class)
    fun configure(settings: ExtMixinConfig)
}
