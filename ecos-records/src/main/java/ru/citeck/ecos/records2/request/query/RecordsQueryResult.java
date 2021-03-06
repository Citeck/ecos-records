package ru.citeck.ecos.records2.request.query;

import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @deprecated -> RecordsQueryRes
 */
@Deprecated
public class RecordsQueryResult<T> extends RecordsResult<T> {

    private boolean hasMore = false;
    private long totalCount = 0;

    public RecordsQueryResult() {
    }

    public RecordsQueryResult(RecordsQueryResult<T> other) {
        super(other);
        hasMore = other.hasMore;
        setTotalCount(other.totalCount);
    }

    public RecordsQueryResult(List<T> records) {
        setRecords(records);
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

    public static <T> RecordsQueryResult<T> of(T... values) {
        RecordsQueryResult<T> result = new RecordsQueryResult<>();
        result.setRecords(Arrays.asList(values));
        return result;
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
        return "{\n"
                    + "  \"records\": [" + recordsStr
                    + "],\n  \"hasMore\": " + getHasMore()
                    + ",\n  \"totalCount\": " + getTotalCount()
                    + debugStr
            + "\n}";
    }
}
