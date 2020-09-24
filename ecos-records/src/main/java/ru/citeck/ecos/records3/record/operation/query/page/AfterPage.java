package ru.citeck.ecos.records3.record.operation.query.page;

import ru.citeck.ecos.records3.RecordRef;

import java.util.Objects;

public class AfterPage extends QueryPage {

    public static final AfterPage DEFAULT = new AfterPage();

    private final RecordRef afterId;

    public AfterPage() {
        this(RecordRef.EMPTY);
    }

    public AfterPage(RecordRef afterId) {
        this.afterId = RecordRef.valueOf(afterId);
    }

    public AfterPage(String afterId) {
        this(RecordRef.valueOf(afterId));
    }

    public AfterPage(RecordRef afterId, Integer maxItems) {
        super(maxItems);
        this.afterId = RecordRef.valueOf(afterId);
    }

    public AfterPage(String afterId, Integer maxItems) {
        this(RecordRef.valueOf(afterId), maxItems);
    }

    public RecordRef getAfterId() {
        return afterId;
    }

    @Override
    public QueryPage withMaxItems(Integer maxItems) {
        return new AfterPage(afterId, maxItems);
    }

    public AfterPage withAfterId(RecordRef afterId) {
        return new AfterPage(afterId, getMaxItems());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AfterPage afterPage = (AfterPage) o;
        return Objects.equals(afterId, afterPage.afterId)
            && Objects.equals(getMaxItems(), afterPage.getMaxItems());
    }

    @Override
    public int hashCode() {
        return Objects.hash(afterId, getMaxItems());
    }

    @Override
    public String toString() {
        return "{\"afterId\":\"" + afterId + "\", \"maxItems\":" + getMaxItems() + '}';
    }
}
