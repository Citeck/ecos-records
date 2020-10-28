package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue


class AttIdValue(id: Any?) : AttValue {
    private val id: String?
    override fun asText(): String? {
        return id
    }

    override fun getId(): String? {
        return id
    }

    init {
        check("id", id)
        this.id = id.toString()
    }
}
