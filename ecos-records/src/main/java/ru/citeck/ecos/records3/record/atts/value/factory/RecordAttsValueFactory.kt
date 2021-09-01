package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.impl.RecordAttValue

class RecordAttsValueFactory : AttValueFactory<RecordAtts> {

    override fun getValue(value: RecordAtts): AttValue {
        return RecordAttValue(value)
    }

    override fun getValueTypes() = listOf(RecordAtts::class.java)
}
