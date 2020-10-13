package ru.citeck.ecos.records3.rest.v1;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;

import java.util.List;

public abstract class RequestBody {

    @Getter @Setter private String requestId;
    @Getter @Setter private List<String> requestTrace;
    @Getter @Setter private MsgLevel msgLevel = MsgLevel.INFO;
}
