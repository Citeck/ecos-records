package ru.citeck.ecos.records3.record.atts.computed.script

import ru.citeck.ecos.webapp.api.entity.EntityRef

typealias ValueScriptContextCreator = (ref: EntityRef) -> AttValueScriptCtx
