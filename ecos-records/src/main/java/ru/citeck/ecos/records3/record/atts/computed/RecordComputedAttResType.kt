package ru.citeck.ecos.records3.record.atts.computed

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class RecordComputedAttResType {
    REF,
    AUTHORITY,
    TEXT,
    MLTEXT,
    @JsonEnumDefaultValue
    ANY
}
