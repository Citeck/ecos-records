package ru.citeck.ecos.records3.record.mixin.provider

import ru.citeck.ecos.records3.record.mixin.AttMixin

interface MutableAttMixinsProvider : AttMixinsProvider {

    fun addMixin(mixin: AttMixin)

    fun addMixins(mixins: Collection<AttMixin>)

    fun removeMixin(mixin: AttMixin)
}
