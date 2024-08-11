package ru.citeck.ecos.records3.record.mixin

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.mixin.provider.AttMixinsProvider
import ru.citeck.ecos.records3.utils.AttUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

class MixinContextImpl() : MixinContext {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private lateinit var exactMixins: Map<String, MixinAttContext>
    private lateinit var endsWithMixins: Map<String, MixinAttContext>

    private val mixins: MutableList<AttMixin> = CopyOnWriteArrayList()
    private val providers: MutableList<AttMixinsProvider> = CopyOnWriteArrayList()

    private val dirty = AtomicBoolean(true)

    private val lock = ReentrantLock()

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

    private fun updateMixins() {
        lock.lock()
        try {
            if (!dirty.get()) {
                return
            }
            updateMixinsImpl()
            dirty.set(false)
        } finally {
            lock.unlock()
        }
    }

    private fun updateMixinsImpl() {
        lock.lock()
        try {
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
        } finally {
            lock.unlock()
        }
    }

    fun addMixin(mixin: AttMixin) {
        lock.lock()
        try {
            addMixins(listOf(mixin))
        } finally {
            lock.unlock()
        }
    }

    fun addMixins(vararg mixins: AttMixin) {
        lock.lock()
        try {
            addMixins(mixins.toList())
        } finally {
            lock.unlock()
        }
    }

    fun addMixins(mixins: Iterable<AttMixin>) {
        lock.lock()
        try {
            this.mixins.addAll(mixins)
            dirty.set(true)
        } finally {
            lock.unlock()
        }
    }

    fun addMixinsProvider(provider: AttMixinsProvider) {
        lock.lock()
        try {
            provider.onChanged { dirty.set(true) }
            providers.add(provider)
            dirty.set(true)
        } finally {
            lock.unlock()
        }
    }
}
