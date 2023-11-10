package ru.citeck.ecos.records3.record.mixin.external

import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.webapp.api.promise.Promise

interface ExtAttMixinContext {

    fun getProvidedAtts(): Set<String>

    fun getRequiredAttsFor(attributes: Set<String>): Set<String>

    fun getAtt(
        handlerContext: ExtAttHandlerContext,
        requiredAtts: Map<String, Any?>,
        attToLoad: SchemaAtt
    ): Promise<Any?>
}
