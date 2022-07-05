package ru.citeck.ecos.records3.record.mixin.provider

import ru.citeck.ecos.records3.record.mixin.AttMixin

interface AttMixinsProvider {

    fun getMixins(): List<AttMixin>

    fun onChanged(action: () -> Unit)
}
