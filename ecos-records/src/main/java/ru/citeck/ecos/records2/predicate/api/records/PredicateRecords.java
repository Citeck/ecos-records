package ru.citeck.ecos.records2.predicate.api.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PredicateRecords extends LocalRecordsDao implements LocalRecordsQueryWithMetaDao<Object> {

    public static final String ID = "predicate";

    public PredicateRecords() {
        setId(ID);
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery recordsQuery, @NotNull MetaField field) {

        PredicateCheckQuery query = recordsQuery.getQuery(PredicateCheckQuery.class);

        if (query.getPredicates().isEmpty() || query.getRecords().isEmpty()) {
            return new RecordsQueryResult<>();
        }

        RecordsQueryResult<Object> queryResult = new RecordsQueryResult<>();

        for (RecordRef record : query.getRecords()) {

            List<Boolean> checkResult = new ArrayList<>();
            RecordElement element = new RecordElement(recordsService, record);

            for (Predicate predicate : query.getPredicates()) {
                checkResult.add(predicateService.isMatch(element, predicate));
            }

            queryResult.addRecord(new PredicateCheckResult(record, checkResult));
        }

        return queryResult;
    }

    @Data
    @RequiredArgsConstructor
    public static class PredicateCheckResult {
        private final RecordRef record;
        private final List<Boolean> result;
    }

    @Data
    public static class PredicateCheckQuery {

        @NotNull
        private List<RecordRef> records = Collections.emptyList();
        @NotNull
        private List<Predicate> predicates = Collections.emptyList();

        public void setRecord(RecordRef recordRef) {
            records = Collections.singletonList(recordRef);
        }

        public void setPredicate(Predicate predicate) {
            predicates = Collections.singletonList(predicate);
        }
    }
}
