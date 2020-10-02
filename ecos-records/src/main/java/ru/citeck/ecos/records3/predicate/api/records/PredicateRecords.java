package ru.citeck.ecos.records3.predicate.api.records;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.operation.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.meta.util.AttModelUtils;
import ru.citeck.ecos.records3.record.operation.meta.util.RecordModelAtts;
import ru.citeck.ecos.records3.predicate.PredicateUtils;
import ru.citeck.ecos.records3.predicate.RecordElement;
import ru.citeck.ecos.records3.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.*;

@SuppressFBWarnings(value = {
    "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"
})
public class PredicateRecords extends AbstractRecordsDao implements RecordsQueryDao {

    public static final String ID = "predicate";

    public PredicateRecords() {
        setId(ID);
    }

    @Override
    public RecordsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {

        PredicateCheckQuery query = recordsQuery.getQuery(PredicateCheckQuery.class);

        if (query.getPredicates().isEmpty() || query.getRecords().isEmpty()) {
            return new RecordsQueryRes<>();
        }

        RecordsQueryRes<Object> queryResult = new RecordsQueryRes<>();

        Set<String> attributes = new HashSet<>();
        for (Predicate predicate : query.getPredicates()) {
            attributes.addAll(PredicateUtils.getAllPredicateAttributes(predicate));
        }

        RecordModelAtts recordModelAtts = AttModelUtils.splitModelAttributes(attributes);

        List<RecordAtts> resolvedAtts = recordsService.getAtts(
            query.getRecords(),
            recordModelAtts.getRecordAtts()
        );
        if (!recordModelAtts.getModelAtts().isEmpty()) {
            RecordAtts modelAtts = recordsService.getAtts(
                RequestContext.getCurrentNotNull().getAttributes(),
                recordModelAtts.getModelAtts()
            );
            resolvedAtts.forEach(rec -> modelAtts.forEach(rec::set));
        }

        int idx = 0;
        for (RecordAtts record : resolvedAtts) {

            List<Boolean> checkResult = new ArrayList<>();
            RecordElement element = new RecordElement(record);

            for (Predicate predicate : query.getPredicates()) {
                Predicate resolved = PredicateUtils.resolvePredicateWithAttributes(predicate, record.getAttributes());
                checkResult.add(predicateService.isMatch(element, resolved));
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
