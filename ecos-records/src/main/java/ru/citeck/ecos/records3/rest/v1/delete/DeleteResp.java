package ru.citeck.ecos.records3.rest.v1.delete;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;
import ru.citeck.ecos.records3.rest.v1.RequestResp;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DeleteResp extends RequestResp {

    private List<DelStatus> statuses = new ArrayList<>();

    public void setStatuses(List<DelStatus> statuses) {
        if (statuses != null) {
            this.statuses = new ArrayList<>(statuses);
        } else {
            this.statuses.clear();
        }
        this.statuses = statuses;
    }
}
