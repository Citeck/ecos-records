package ru.citeck.ecos.records3.record.atts.computed

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class RecordComputedAttType {
    SCRIPT,
    ATTRIBUTE,
    VALUE,
    TEMPLATE,
    @JsonEnumDefaultValue
    NONE
}
