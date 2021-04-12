package ru.citeck.ecos.records3.record.request.msg

enum class MsgLevel {

    ERROR,
    WARN,
    DEBUG;

    fun isEnabled(other: MsgLevel): Boolean {
        return ordinal >= other.ordinal
    }
}
