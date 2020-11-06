package ru.citeck.ecos.records3.record.request.msg

enum class MsgLevel(
    val order: Int,
    val allowedForMsg: Boolean
) {

    OFF(0, false),
    ERROR(2, true),
    WARN(3, true),
    INFO(4, true),
    DEBUG(5, true),
    TRACE(6, true);

    fun isEnabled(other: MsgLevel): Boolean {
        return order >= other.order
    }
}