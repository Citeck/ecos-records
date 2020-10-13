package ru.citeck.ecos.records3.rest.v1;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class RequestBody {

    private @NotNull String requestId = "";
    private @NotNull List<String> requestTrace = new ArrayList<>();
    private @NotNull MsgLevel msgLevel = MsgLevel.INFO;

    public void setRequestTrace(List<String> requestTrace) {
        if (requestTrace != null) {
            this.requestTrace = new ArrayList<>(requestTrace);
        } else {
            this.requestTrace = new ArrayList<>();
        }
    }

    public int getVersion() {
        return 1;
    }

    @Override
    public String toString() {
        return Json.getMapper().toString(this);
    }
}
