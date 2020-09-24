package ru.citeck.ecos.records3.record.op.query.request.query.page;

import java.util.Objects;

public class SkipPage extends QueryPage {

    public static final SkipPage DEFAULT = new SkipPage();

    private final int skipCount;

    public SkipPage() {
        skipCount = 0;
    }

    public SkipPage(Integer skipCount, Integer maxItems) {
        super(maxItems);
        this.skipCount = skipCount != null ? skipCount : 0;
    }

    public int getSkipCount() {
        return skipCount;
    }

    @Override
    public QueryPage withMaxItems(Integer maxItems) {
        return new SkipPage(skipCount, maxItems);
    }

    public SkipPage withSkipCount(Integer skipCount) {
        return new SkipPage(skipCount, getMaxItems());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SkipPage skipPage = (SkipPage) o;
        return skipCount == skipPage.skipCount
            && getMaxItems() == skipPage.getMaxItems();
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipCount, getMaxItems());
    }

    @Override
    public String toString() {
        return "{\"skipCount\":" + skipCount + ", \"maxItems\":" + getMaxItems() + '}';
    }
}
