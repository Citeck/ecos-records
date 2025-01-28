package ru.citeck.ecos.records3.record.mixin.external

import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt

class ExtAttData(
    val typeId: String,
    val local: Boolean,
    val priority: Float,
    val attribute: String,
    val requiredAtts: Map<String, String>,
    val configurer: ExtAttMixinConfigurer,
    val handler: (ExtAttHandlerContext, Map<String, Any?>, SchemaAtt) -> Any?
)
