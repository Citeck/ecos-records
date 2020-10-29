package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class AttIdValue(private val id: Any?) : AttValue {

    override fun asText(): String? {
        return id?.toString() ?: ""
    }

    override fun getId(): Any {
        return id ?: ""
    }
}
