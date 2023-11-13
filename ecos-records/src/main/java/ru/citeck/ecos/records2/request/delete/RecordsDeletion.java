package ru.citeck.ecos.records2.request.delete;

import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class RecordsDeletion {

    private List<EntityRef> records = new ArrayList<>();
    private boolean debug = false;

    public List<EntityRef> getRecords() {
        return records;
    }

    public void setRecords(List<EntityRef> records) {
        if (records != null) {
            this.records = new ArrayList<>(records);
        } else {
            this.records.clear();
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
