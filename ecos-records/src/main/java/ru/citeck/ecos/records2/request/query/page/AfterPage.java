package ru.citeck.ecos.records2.request.query.page;

import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Objects;

public class AfterPage extends QueryPage {

    public static final AfterPage DEFAULT = new AfterPage();

    private final EntityRef afterId;

    public AfterPage() {
        this(EntityRef.EMPTY);
    }

    public AfterPage(EntityRef afterId) {
        this.afterId = EntityRef.valueOf(afterId);
    }

    public AfterPage(String afterId) {
        this(EntityRef.valueOf(afterId));
    }

    public AfterPage(EntityRef afterId, Integer maxItems) {
        super(maxItems);
        this.afterId = EntityRef.valueOf(afterId);
    }

    public AfterPage(String afterId, Integer maxItems) {
        this(EntityRef.valueOf(afterId), maxItems);
    }

    public EntityRef getAfterId() {
        return afterId;
    }

    @Override
    public QueryPage withMaxItems(Integer maxItems) {
        return new AfterPage(afterId, maxItems);
    }

    public AfterPage withAfterId(EntityRef afterId) {
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
