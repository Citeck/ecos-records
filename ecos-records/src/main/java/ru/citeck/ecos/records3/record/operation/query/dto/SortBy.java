package ru.citeck.ecos.records3.record.operation.query.dto;

import java.util.Objects;

public class SortBy {

    private String attribute;
    private boolean ascending;

    public SortBy() {
    }

    public SortBy(String attribute, boolean ascending) {
        this.attribute = attribute;
        this.ascending = ascending;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public boolean isAscending() {
        return ascending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SortBy sortBy = (SortBy) o;
        return ascending == sortBy.ascending
            && Objects.equals(attribute, sortBy.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, ascending);
    }

    @Override
    public String toString() {
        return "SortBy{"
            + "attribute='" + attribute + '\''
            + ", ascending=" + ascending
            + '}';
    }
}
