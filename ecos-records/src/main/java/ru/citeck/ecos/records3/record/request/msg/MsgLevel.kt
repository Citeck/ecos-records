package ru.citeck.ecos.records3.record.request.msg

import ecos.com.fasterxml.jackson210.annotation.JsonEnumDefaultValue

enum class MsgLevel {

    ERROR,
    WARN,
    @JsonEnumDefaultValue
    INFO,
    DEBUG;

    fun isEnabled(other: MsgLevel): Boolean {
        return ordinal >= other.ordinal
    }
}
