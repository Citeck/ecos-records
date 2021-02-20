package ru.citeck.ecos.records3.record.atts.schema.read.proc

import ru.citeck.ecos.records3.record.atts.proc.AttProcDef

data class AttWithProc(
    val attribute: String,
    val processors: List<AttProcDef>
)
