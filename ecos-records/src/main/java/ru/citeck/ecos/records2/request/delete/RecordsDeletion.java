package ru.citeck.ecos.records2.request.delete;

import ru.citeck.ecos.records2.RecordRef;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class RecordsDeletion {

    private List<RecordRef> records = new ArrayList<>();
    private boolean debug = false;

    public List<RecordRef> getRecords() {
        return records;
    }

    public void setRecords(List<RecordRef> records) {
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
