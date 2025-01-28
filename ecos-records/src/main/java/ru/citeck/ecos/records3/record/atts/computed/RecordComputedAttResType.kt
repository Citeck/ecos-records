package ru.citeck.ecos.records3.record.atts.computed

import ecos.com.fasterxml.jackson210.annotation.JsonEnumDefaultValue

enum class RecordComputedAttResType {
    REF,
    AUTHORITY,
    TEXT,
    MLTEXT,
    @JsonEnumDefaultValue
    ANY
}
