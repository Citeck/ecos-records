package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.RecordAttValue

class RecordAttValueFactory : AttValueFactory<RecordAtts> {

    override fun getValue(value: RecordAtts): AttValue? {
        return RecordAttValue(value)
    }

    override fun getValueTypes() = listOf(RecordAtts::class.java)
}
