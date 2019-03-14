package ru.citeck.ecos.records2.request.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.page.AfterPage;
import ru.citeck.ecos.records2.request.query.page.QueryPage;
import ru.citeck.ecos.records2.request.query.page.SkipPage;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class RecordsQuery {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String sourceId = "";

    private List<SortBy> sortBy = Collections.emptyList();
    private List<String> groupBy = Collections.emptyList();

    private QueryPage page = new SkipPage();

    private QueryConsistency consistency = QueryConsistency.DEFAULT;
    private String language = "";
    private JsonNode query = MissingNode.getInstance();
    private boolean debug = false;

    public RecordsQuery() {
    }

    public RecordsQuery(RecordsQuery other) {
        this.page = other.page;
        this.query = other.query;
        this.debug = other.debug;
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

    public <T> T getQuery(Class<T> type) {
        try {
            if (query.isTextual()) {
                if (String.class.equals(type)) {
                    return type.cast(query.asText());
                }
                return objectMapper.readValue(query.textValue(), type);
            } else {
                return objectMapper.treeToValue(query, type);
            }
        } catch (IOException e) {
            throw new RuntimeException("Query is incorrect: " + query, e);
        }
    }

    public JsonNode getQuery() {
        return query;
    }

    public void setQuery(Object query) {
        this.query = objectMapper.valueToTree(query);
    }

    @JsonSetter
    public void setQuery(JsonNode query) {
        if (query != null) {
            this.query = query;
        } else {
            this.query = MissingNode.getInstance();
        }
    }

    public void setQuery(String query) {
        if (StringUtils.isNotBlank(query)) {
            this.query = TextNode.valueOf(query);
        } else {
            this.query = MissingNode.getInstance();
        }
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language != null ? language : "";
    }

    @JsonIgnore
    public int getSkipCount() {
        return getSkipPage().getSkipCount();
    }

    @JsonIgnore
    public int getMaxItems() {
        return getPage().getMaxItems();
    }

    @JsonIgnore
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
    public AfterPage getAfterPage() {
        if (page instanceof AfterPage) {
            return (AfterPage) page;
        } else {
            return AfterPage.DEFAULT;
        }
    }

    @JsonIgnore
    public SkipPage getSkipPage() {
        if (page instanceof SkipPage) {
            return (SkipPage) page;
        } else {
            return SkipPage.DEFAULT;
        }
    }

    @JsonIgnore
    public boolean isAfterIdMode() {
        return page instanceof AfterPage;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return debug;
    }

    public List<SortBy> getSortBy() {
        return sortBy;
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

        return Objects.equals(sourceId, that.sourceId)
            && Objects.equals(sortBy, that.sortBy)
            && Objects.equals(page, that.page)
            && Objects.equals(consistency, that.consistency);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(sortBy);
        result = 31 * result + Objects.hashCode(page);
        result = 31 * result + Objects.hashCode(sourceId);
        result = 31 * result + Objects.hashCode(consistency);
        return result;
    }

    @Override
    public String toString() {
        return "RecordsQuery{"
                + "\"sourceId\":\"" + sourceId + "\","
                + "\"sortBy\":\"" + sortBy + "\","
                + "\"consistency\":\"" + consistency + "\","
                + "\"language\":\"" + language + '\'' + "\","
                + "\"query\":\"" + query + "\","
                + "\"page\":\"" + page + "\""
            + '}';
    }
}
