package ru.citeck.ecos.records3.record.atts.computed

import ecos.com.fasterxml.jackson210.annotation.JsonEnumDefaultValue

enum class RecordComputedAttType {
    SCRIPT,
    ATTRIBUTE,
    VALUE,
    TEMPLATE,
    @JsonEnumDefaultValue
    NONE
}
