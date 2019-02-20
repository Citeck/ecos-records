package ru.citeck.ecos.records2.request.mutation;

import ru.citeck.ecos.records2.RecordMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordsMutation {

    private List<RecordMeta> records = new ArrayList<>();

    private boolean debug = false;

    public RecordsMutation() {
    }

    public RecordsMutation(RecordsMutation other, Function<RecordMeta, RecordMeta> convert) {
        this.debug = other.debug;
        this.records = other.getRecords()
                            .stream()
                            .map(convert)
                            .collect(Collectors.toList());
    }

    public List<RecordMeta> getRecords() {
        return records;
    }

    public void setRecords(List<RecordMeta> records) {
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
