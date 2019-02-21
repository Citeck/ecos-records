package ru.citeck.ecos.records2.request.query;

import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordsQueryResult<T> extends RecordsResult<T> {

    private boolean hasMore = false;
    private long totalCount = 0;

    private boolean defaultTotal = true;

    public RecordsQueryResult() {
    }

    public RecordsQueryResult(RecordsQueryResult<T> other) {
        super(other);
        hasMore = other.hasMore;
        setTotalCount(other.totalCount);
    }

    public <K> RecordsQueryResult(RecordsQueryResult<K> other, Function<K, T> mapper) {
        super(other, mapper);
        hasMore = other.hasMore;
        setTotalCount(other.totalCount);
    }

    public void merge(RecordsQueryResult<T> other) {
        super.merge(other);
        hasMore = other.getHasMore();
        totalCount += other.getTotalCount();
    }

    public boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
        defaultTotal = false;
    }

    @Override
    public void setRecords(List<T> records) {
        super.setRecords(records);
        if (defaultTotal) {
            totalCount = getRecords().size();
        }
    }

    @Override
    public void addRecord(T record) {
        super.addRecord(record);
        if (defaultTotal) {
            totalCount = getRecords().size();
        }
    }

    @Override
    public String toString() {
        String recordsStr = getRecords().stream()
                                   .map(Object::toString)
                                   .collect(Collectors.joining(",\n    "));
        if (!recordsStr.isEmpty()) {
            recordsStr = "\n    " + recordsStr + "\n  ";
        }
        String debugStr = getDebug() != null ? ",\n  \"debug\": " + getDebug() : "";
        return "{\n" +
                "  \"records\": [" + recordsStr +
                "],\n  \"hasMore\": " + hasMore +
                ",\n  \"totalCount\": " + totalCount +
                debugStr +
                "\n}";
    }
}
