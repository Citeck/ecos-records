package ru.citeck.ecos.records3.record.request.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.DataValue;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestMsg {
    private MsgLevel level;
    private Instant time;
    private String type;
    private DataValue msg;
    private List<String> queryTrace;
}
