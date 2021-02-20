package ru.citeck.ecos.records3.record.mixin.impl.mutmeta

import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.*

class MutMetaMixin(
    val id: String,
    private val getMutMetaByLocalId: ((String) -> MutMeta?)? = null
) : AttMixin {

    companion object {
        private val PROVIDED_ATTS = setOf(
            RecordConstants.ATT_MODIFIER,
            RecordConstants.ATT_MODIFIED,
            RecordConstants.ATT_CREATOR,
            RecordConstants.ATT_CREATED
        )
    }

    private val mutMetaCacheKey = this::class.java.simpleName + ".mut-meta.$id"

    fun addCtxMeta(localId: String, meta: MutMeta) {
        val ctx = RequestContext.getCurrent()
        if (ctx != null) {
            val metaByIdCache = ctx.getMap<String, Optional<MutMeta>>(mutMetaCacheKey)
            metaByIdCache[localId] = Optional.of(meta)
        }
    }

    override fun getAtt(path: String, value: AttValueCtx): Any? {

        val ctx = RequestContext.getCurrentNotNull()
        val metaByIdCache = ctx.getMap<String, Optional<MutMeta>>(mutMetaCacheKey)

        val metaOpt = metaByIdCache.computeIfAbsent(value.getLocalId()) {
            Optional.ofNullable(getMutMetaByLocalId?.invoke(it))
        }
        if (!metaOpt.isPresent) {
            return null
        }

        val meta = metaOpt.get()

        return when (path) {
            RecordConstants.ATT_MODIFIER -> meta.modifier
            RecordConstants.ATT_MODIFIED -> meta.modified
            RecordConstants.ATT_CREATOR -> meta.creator
            RecordConstants.ATT_CREATED -> meta.created
            else -> null
        }
    }

    override fun getProvidedAtts(): Collection<String> {
        return PROVIDED_ATTS
    }
}
