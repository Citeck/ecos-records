package ru.citeck.ecos.records3.record.operation.delete;

import lombok.Data;
import ru.citeck.ecos.records3.record.request.msg.RequestMsg;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeleteResp {

    private List<DelStatus> statuses = new ArrayList<>();
    private List<RequestMsg> messages = new ArrayList<>();

    public List<DelStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<DelStatus> statuses) {
        if (statuses != null) {
            this.statuses = new ArrayList<>(statuses);
        } else {
            this.statuses.clear();
        }
        this.statuses = statuses;
    }

    public List<RequestMsg> getMessages() {
        return messages;
    }

    public void setMessages(List<RequestMsg> messages) {
        if (messages != null) {
            this.messages = new ArrayList<>(messages);
        } else {
            this.messages.clear();
        }
        this.messages = messages;
    }
}
