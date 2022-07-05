package ru.citeck.ecos.records3.record.mixin

import mu.KotlinLogging
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.mixin.provider.AttMixinsProvider
import ru.citeck.ecos.records3.utils.AttUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class MixinContextImpl() : MixinContext {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private lateinit var exactMixins: Map<String, MixinAttContext>
    private lateinit var endsWithMixins: Map<String, MixinAttContext>

    private val mixins: MutableList<AttMixin> = CopyOnWriteArrayList()
    private val providers: MutableList<AttMixinsProvider> = CopyOnWriteArrayList()

    private val dirty = AtomicBoolean(true)

    constructor(vararg mixins: AttMixin) : this() {
        addMixins(mixins.toList())
    }

    constructor(mixins: Iterable<AttMixin>) : this() {
        addMixins(mixins)
    }

    override fun getMixin(path: String): MixinAttContext? {
        if (dirty.get()) {
            updateMixins()
        }
        val exactMixin = exactMixins[path]
        if (exactMixin != null) {
            return exactMixin
        }
        for (att in endsWithMixins.entries) {
            if (path.endsWith(att.key)) {
                return att.value
            }
        }
        return null
    }

    @Synchronized
    private fun updateMixins() {
        if (!dirty.compareAndSet(true, false)) {
            return
        }
        try {
            updateMixinsImpl()
        } catch (e: Exception) {
            dirty.set(true)
            throw e
        }
    }

    @Synchronized
    private fun updateMixinsImpl() {

        val mixins = ArrayList(providers.flatMap { it.getMixins() })
        mixins.addAll(this.mixins)

        val newExactMixins: MutableMap<String, MixinAttContext> = HashMap()
        val newEndsWithMixins: MutableMap<String, MixinAttContext> = HashMap()

        for (mixin in mixins) {

            mixin.getProvidedAtts().forEach { att ->

                if (AttUtils.isValidComputedAtt(att, true)) {

                    val mixinAttCtx = MixinAttContext(mixin, att)

                    if (att[0] == '*') {

                        newEndsWithMixins[att.substring(1)] = mixinAttCtx
                    } else {

                        newExactMixins[att] = mixinAttCtx

                        val lastDotIdx = att.lastIndexOf('.')
                        if (lastDotIdx == -1) {
                            val scalar = ScalarType.getByMirrorAtt(att)
                            if (scalar != null) {
                                newExactMixins[scalar.schema] = mixinAttCtx
                            }
                        } else if (att.length > lastDotIdx + 1) {
                            val scalar = ScalarType.getByMirrorAtt(att.substring(lastDotIdx + 1))
                            if (scalar != null) {
                                newExactMixins[att.substring(0, lastDotIdx) + scalar.schema] = mixinAttCtx
                            }
                        }
                    }
                } else {
                    log.warn { "Incorrect computed att: '$att'" }
                }
            }
        }

        this.exactMixins = newExactMixins
        this.endsWithMixins = newEndsWithMixins
    }

    fun addMixin(mixin: AttMixin) {
        addMixins(listOf(mixin))
    }

    fun addMixins(vararg mixins: AttMixin) {
        addMixins(mixins.toList())
    }

    fun addMixins(mixins: Iterable<AttMixin>) {
        this.mixins.addAll(mixins)
        dirty.set(true)
    }

    fun addMixinsProvider(provider: AttMixinsProvider) {
        provider.onChanged { dirty.set(true) }
        providers.add(provider)
        dirty.set(true)
    }
}
