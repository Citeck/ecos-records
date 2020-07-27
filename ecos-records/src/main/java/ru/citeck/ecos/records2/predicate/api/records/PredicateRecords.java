package ru.citeck.ecos.records2.predicate.api.records;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.meta.util.AttModelUtils;
import ru.citeck.ecos.records2.meta.util.RecordModelAtts;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;

import java.util.*;

@SuppressFBWarnings(value = {
    "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"
})
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

        Set<String> attributes = new HashSet<>();
        for (Predicate predicate : query.getPredicates()) {
            attributes.addAll(PredicateUtils.getAllPredicateAttributes(predicate));
        }

        RecordModelAtts recordModelAtts = AttModelUtils.splitModelAttributes(attributes);

        RecordsResult<RecordMeta> resolvedAtts = recordsService.getAttributes(
            query.getRecords(),
            recordModelAtts.getRecordAtts()
        );
        if (!recordModelAtts.getModelAtts().isEmpty()) {
            RecordMeta modelAtts = recordsMetaService.getMeta(
                QueryContext.getCurrent().getAttributes(),
                recordModelAtts.getModelAtts()
            );
            resolvedAtts.getRecords().forEach(rec -> modelAtts.forEach(rec::set));
        }

        int idx = 0;
        for (RecordMeta record : resolvedAtts.getRecords()) {

            List<Boolean> checkResult = new ArrayList<>();
            RecordElement element = new RecordElement(record);

            for (Predicate predicate : query.getPredicates()) {
                checkResult.add(predicateService.isMatch(element, predicate));
            }

            RecordRef recordRef = query.getRecords().get(idx++);
            queryResult.addRecord(new PredicateCheckResult(recordRef, checkResult));
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
