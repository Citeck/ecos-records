package ru.citeck.ecos.records2.request.result;

import ru.citeck.ecos.records2.request.error.RecordsError;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordsResult<T> extends DebugResult {

    private List<T> records = new ArrayList<>();
    private List<RecordsError> errors = new ArrayList<>();

    /* ==== constructor ====*/

    public RecordsResult() {
    }

    public RecordsResult(RecordsResult<T> other) {
        super(other);
        setErrors(other.getErrors());
        setRecords(other.getRecords());
    }

    public <K> RecordsResult(RecordsResult<K> other, Function<K, T> mapper) {
        super(other);
        setErrors(other.getErrors());
        setRecords(mapNotNull(other.getRecords(), mapper));
    }

    public <K> RecordsResult(List<K> records, Function<K, T> mapper) {
        setRecords(mapNotNull(records, mapper));
    }

    public RecordsResult(List<T> records) {
        setRecords(records);
    }

    /* ==== /constructor ==== */

    /* ======= records ====== */

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = new ArrayList<>();
        addRecords(records);
    }

    public void addRecord(T record) {
        records.add(record);
    }

    public void addRecords(Collection<T> records) {
        if (records != null) {
            this.records.addAll(records);
        }
    }

    /* ====== /records ====== */

    /* ======= errors ======= */

    public List<RecordsError> getErrors() {
        return errors;
    }

    public void setErrors(List<RecordsError> errors) {
        this.errors = new ArrayList<>();
        addErrors(errors);
    }

    public void addError(RecordsError error) {
        errors.add(error);
    }

    public void addErrors(Collection<RecordsError> errors) {
        if (errors != null) {
            this.errors.addAll(errors);
        }
    }

    /* ====== /errors ======= */

    private <I, O> List<O> mapNotNull(List<I> input, Function<I, O> mapper) {
        if (input == null) {
            return Collections.emptyList();
        }
        return input.stream()
                    .map(r -> Optional.ofNullable(mapper.apply(r)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
    }

    public void merge(RecordsResult<T> other) {
        super.merge(other);
        addErrors(other.getErrors());
        addRecords(other.getRecords());
    }

    @Override
    public String toString() {
        return records.stream()
                      .map(Object::toString)
                      .collect(Collectors.joining(",\n"));
    }
}
