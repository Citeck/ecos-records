package ru.citeck.ecos.records3.record.op.query;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import ecos.com.fasterxml.jackson210.annotation.JsonSetter;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.QueryConsistency;
import ru.citeck.ecos.records2.request.query.SortBy;
import ru.citeck.ecos.records2.request.query.page.AfterPage;
import ru.citeck.ecos.records2.request.query.page.QueryPage;
import ru.citeck.ecos.records2.request.query.page.SkipPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordsQuery {

    private String sourceId = "";

    private List<SortBy> sortBy = Collections.emptyList();
    private List<String> groupBy = Collections.emptyList();

    private QueryPage page = new SkipPage();

    private QueryConsistency consistency = QueryConsistency.DEFAULT;
    private String language = "";
    private JsonNode query = null;

    public RecordsQuery() {
    }

    public RecordsQuery(RecordsQuery other) {
        this.page = other.page;
        this.query = Json.getMapper().copy(other.query);
        this.language = other.language;
        this.sourceId = other.sourceId;
        this.consistency = other.consistency;
        this.sortBy = new ArrayList<>(other.sortBy);
        this.groupBy = new ArrayList<>(other.groupBy);
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public <T> T getQueryOrNull(Class<T> type) {
        try {
            return getQuery(type);
        } catch (Exception e) {
            log.debug("Can't convert query to type " + type + ". Query: " + query, e);
            return null;
        }
    }

    public <T> T getQuery(Class<T> type) {
        return Json.getMapper().convert(query, type);
    }

    public Object getQuery() {
        return Json.getMapper().toJava(query);
    }

    @JsonSetter
    @com.fasterxml.jackson.annotation.JsonSetter
    public void setQuery(Object query) {
        this.query = Json.getMapper().toJson(query);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language != null ? language : "";
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getSkipCount() {
        return getSkipPage().getSkipCount();
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getMaxItems() {
        return getPage().getMaxItems();
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public RecordRef getAfterId() {
        return getAfterPage().getAfterId();
    }

    public void setAfterId(RecordRef afterId) {
        setPage(getAfterPage().withAfterId(afterId));
    }

    public void setSkipCount(Integer skipCount) {
        setPage(getSkipPage().withSkipCount(skipCount));
    }

    public void setMaxItems(Integer maxItems) {
        setPage(getPage().withMaxItems(maxItems));
    }

    public QueryPage getPage() {
        return page;
    }

    public void setPage(QueryPage page) {
        this.page = page != null ? page : new SkipPage();
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public AfterPage getAfterPage() {
        if (page instanceof AfterPage) {
            return (AfterPage) page;
        } else {
            return AfterPage.DEFAULT;
        }
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public SkipPage getSkipPage() {
        if (page instanceof SkipPage) {
            return (SkipPage) page;
        } else {
            return SkipPage.DEFAULT;
        }
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAfterIdMode() {
        return page instanceof AfterPage;
    }

    public List<SortBy> getSortBy() {
        return sortBy;
    }

    public void addSort(SortBy sort) {
        if (this.sortBy == null) {
            this.sortBy = new ArrayList<>();
        } else if (!(this.sortBy instanceof ArrayList)) {
            this.sortBy = new ArrayList<>(this.sortBy);
        }
        this.sortBy.add(sort);
    }

    public void setSortBy(List<SortBy> sortBy) {
        this.sortBy = sortBy != null ? sortBy : Collections.emptyList();
    }

    public List<String> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(List<String> groupBy) {
        this.groupBy = groupBy != null ? groupBy : Collections.emptyList();
    }

    public QueryConsistency getConsistency() {
        return consistency;
    }

    public void setConsistency(QueryConsistency consistency) {
        this.consistency = consistency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RecordsQuery that = (RecordsQuery) o;

        return Objects.equals(page, that.page)
            && Objects.equals(query, that.query)
            && Objects.equals(sortBy, that.sortBy)
            && Objects.equals(groupBy, that.groupBy)
            && Objects.equals(language, that.language)
            && Objects.equals(sourceId, that.sourceId)
            && Objects.equals(consistency, that.consistency);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(page);
        result = 31 * result + Objects.hashCode(query);
        result = 31 * result + Objects.hashCode(sortBy);
        result = 31 * result + Objects.hashCode(groupBy);
        result = 31 * result + Objects.hashCode(language);
        result = 31 * result + Objects.hashCode(sourceId);
        result = 31 * result + Objects.hashCode(consistency);
        return result;
    }

    @Override
    public String toString() {
        return Json.getMapper().toString(this);
    }
}
