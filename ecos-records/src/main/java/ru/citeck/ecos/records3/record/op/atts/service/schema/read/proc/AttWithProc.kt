package ru.citeck.ecos.records3.record.op.atts.service.schema.read.proc

import ru.citeck.ecos.records3.record.op.atts.service.proc.AttProcDef

data class AttWithProc(
    val attribute: String,
    val processors: List<AttProcDef>
)
