package ru.citeck.ecos.records3.record.mixin.provider

import ru.citeck.ecos.records3.record.mixin.AttMixin
import java.util.*
import kotlin.collections.ArrayList

class AttMixinsProviderImpl : MutableAttMixinsProvider {

    private val mixins: MutableList<AttMixin> = Collections.synchronizedList(ArrayList())
    private val listeners: MutableList<() -> Unit> = Collections.synchronizedList(ArrayList())

    @Synchronized
    override fun getMixins(): List<AttMixin> {
        return mixins
    }

    @Synchronized
    override fun addMixin(mixin: AttMixin) {
        mixins.add(mixin)
        listeners.forEach { it.invoke() }
    }

    @Synchronized
    override fun addMixins(mixins: Collection<AttMixin>) {
        this.mixins.addAll(mixins)
        listeners.forEach { it.invoke() }
    }

    @Synchronized
    override fun removeMixin(mixin: AttMixin) {
        mixins.remove(mixin)
        listeners.forEach { it.invoke() }
    }

    override fun onChanged(action: () -> Unit) {
        listeners.add(action)
    }
}
