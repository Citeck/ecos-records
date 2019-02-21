package ru.citeck.ecos.records2.request.result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordsResult<T> extends DebugResult {

    private List<T> records = new ArrayList<>();

    public RecordsResult() {
    }

    public RecordsResult(RecordsResult<T> other) {
        super(other);
        records = new ArrayList<>(other.getRecords());
    }

    public <K> RecordsResult(RecordsResult<K> other, Function<K, T> mapper) {
        super(other);
        records = new ArrayList<>();
        for (K record : other.getRecords()) {
            T mappedRec = mapper.apply(record);
            if (mappedRec != null) {
                records.add(mappedRec);
            }
        }
    }

    public <K> RecordsResult(List<K> records, Function<K, T> mapper) {
        setRecords(records.stream().map(mapper).collect(Collectors.toList()));
    }

    public RecordsResult(List<T> records) {
        setRecords(records);
    }


    public void merge(RecordsResult<T> other) {
        super.merge(other);

        List<T> records = new ArrayList<>();
        records.addAll(this.records);
        records.addAll(other.getRecords());
        this.records = records;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        if (records != null) {
            this.records = new ArrayList<>(records);
        } else {
            this.records = new ArrayList<>();
        }
    }

    public void addRecord(T record) {
        records.add(record);
    }

    @Override
    public String toString() {
        return records.stream()
                      .map(Object::toString)
                      .collect(Collectors.joining(",\n"));
    }
}
