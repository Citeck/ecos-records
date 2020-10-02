package ru.citeck.ecos.records3.record.request.msg;

import lombok.Getter;

public enum MsgLevel {

    OFF(0, false),
    ERROR(2, true),
    WARN(3, true),
    INFO(4, true),
    DEBUG(5, true),
    TRACE(6, true);

    private final int order;
    @Getter
    private final boolean allowedForMsg;

    MsgLevel(int order, boolean allowedForMsg) {
        this.order = order;
        this.allowedForMsg = allowedForMsg;
    }

    public boolean isEnabled(MsgLevel other) {
        return this.order >= other.order;
    }
}
