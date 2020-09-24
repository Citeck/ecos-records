package ru.citeck.ecos.records3.record.op.query.request.query;

import lombok.Data;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class RecsQueryRes<T> {

    private List<T> records = new ArrayList<>();
    private boolean hasMore = false;
    private long totalCount = 0;

    public RecsQueryRes() {
    }

    public RecsQueryRes(RecsQueryRes<T> other) {
        hasMore = other.hasMore;
        setTotalCount(other.totalCount);
        setRecords(other.getRecords());
    }

    public RecsQueryRes(List<T> records) {
        setRecords(records);
    }

    public <K> RecsQueryRes(RecsQueryRes<K> other, Function<K, T> mapper) {
        setHasMore(other.hasMore);
        setTotalCount(other.totalCount);
        setRecords(mapNotNull(other.getRecords(), mapper));
    }

    public void merge(RecsQueryRes<T> other) {
        hasMore = hasMore || other.getHasMore();
        totalCount += other.getTotalCount();
        addRecords(other.getRecords());
    }

    public boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
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
    public static <T> RecsQueryRes<T> of(T... values) {
        RecsQueryRes<T> result = new RecsQueryRes<>();
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
        String recordsStr = getRecords().stream()
                                   .map(Object::toString)
                                   .collect(Collectors.joining(",\n    "));
        if (!recordsStr.isEmpty()) {
            recordsStr = "\n    " + recordsStr + "\n  ";
        }
        return "{\n"
                    + "  \"records\": [" + recordsStr
                    + "],\n  \"hasMore\": " + getHasMore()
                    + ",\n  \"totalCount\": " + getTotalCount()
            + "\n}";
    }
}
