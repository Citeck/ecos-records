package ru.citeck.ecos.records3.rest.v1;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.request.msg.RequestMsg;

import java.util.ArrayList;
import java.util.List;

public abstract class RequestResp {

    @Getter
    @NotNull
    private List<RequestMsg> messages = new ArrayList<>();

    public void setMessages(List<RequestMsg> messages) {
        if (messages != null) {
            this.messages = new ArrayList<>(messages);
        } else {
            this.messages.clear();
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
