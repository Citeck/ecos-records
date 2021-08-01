package ru.citeck.ecos.records3.record.mixin

import mu.KotlinLogging
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.utils.AttUtils

class MixinContext() {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private lateinit var exactMixins: Map<String, MixinAttContext>
    private lateinit var endsWithMixins: Map<String, MixinAttContext>

    private var initialized = false

    constructor(vararg mixins: AttMixin) : this() {
        addMixins(mixins.toList())
    }

    constructor(mixins: Iterable<AttMixin>) : this() {
        addMixins(mixins)
    }

    fun getMixin(path: String): MixinAttContext? {
        if (!initialized) {
            return null
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

    fun addMixin(mixin: AttMixin) {
        addMixins(listOf(mixin))
    }

    fun addMixins(vararg mixins: AttMixin) {
        addMixins(mixins.toList())
    }

    fun addMixins(mixins: Iterable<AttMixin>) {

        val newExactMixins: MutableMap<String, MixinAttContext>
        val newEndsWithMixins: MutableMap<String, MixinAttContext>

        if (initialized) {
            newExactMixins = HashMap(exactMixins)
            newEndsWithMixins = HashMap(endsWithMixins)
        } else {
            newExactMixins = HashMap()
            newEndsWithMixins = HashMap()
        }

        for (mixin in mixins) {

            mixin.getProvidedAtts().forEach { att ->

                if (AttUtils.isValidComputedAtt(att)) {

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
        initialized = true
    }

    class MixinAttContext(
        val mixin: AttMixin,
        val path: String
    )
}
