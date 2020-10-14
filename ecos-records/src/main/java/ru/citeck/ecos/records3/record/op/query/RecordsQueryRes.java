package ru.citeck.ecos.records3.record.op.query;

import lombok.Data;
import ru.citeck.ecos.commons.json.Json;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class RecordsQueryRes<T> {

    private List<T> records = new ArrayList<>();
    private boolean hasMore = false;
    private long totalCount = 0;

    public RecordsQueryRes() {
    }

    public RecordsQueryRes(RecordsQueryRes<T> other) {
        hasMore = other.hasMore;
        setTotalCount(other.totalCount);
        setRecords(other.getRecords());
    }

    public RecordsQueryRes(List<T> records) {
        setRecords(records);
    }

    public <K> RecordsQueryRes(RecordsQueryRes<K> other, Function<K, T> mapper) {
        setHasMore(other.hasMore);
        setTotalCount(other.totalCount);
        setRecords(mapNotNull(other.getRecords(), mapper));
    }

    public void merge(RecordsQueryRes<T> other) {
        hasMore = hasMore || other.isHasMore();
        totalCount += other.getTotalCount();
        addRecords(other.getRecords());
    }

    /**
     * Get total records count.
     *
     * @return >= getRecords().size()
     */
    public long getTotalCount() {
        return Math.max(totalCount, getRecords().size());
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

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

    @SafeVarargs
    public static <T> RecordsQueryRes<T> of(T... values) {
        RecordsQueryRes<T> result = new RecordsQueryRes<>();
        result.setRecords(Arrays.asList(values));
        return result;
    }

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

    @Override
    public String toString() {
        return Json.getMapper().toString(this);
    }
}
