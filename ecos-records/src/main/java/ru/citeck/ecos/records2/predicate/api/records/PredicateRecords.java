package ru.citeck.ecos.records2.predicate.api.records;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.*;

@SuppressFBWarnings(value = {
    "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"
})
public class PredicateRecords extends AbstractRecordsDao implements RecordsQueryDao {

    public static final String ID = "predicate";

    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {

        PredicateCheckQuery query = recordsQuery.getQuery(PredicateCheckQuery.class);

        if (query.getPredicates().isEmpty() || query.getRecords().isEmpty()) {
            return new RecsQueryRes<>();
        }

        RecsQueryRes<Object> queryResult = new RecsQueryRes<>();

        Set<String> attributes = new HashSet<>();
        for (Predicate predicate : query.getPredicates()) {
            attributes.addAll(PredicateUtils.getAllPredicateAttributes(predicate));
        }

        List<RecordAtts> resolvedAtts = recordsService.getAtts(query.getRecords(), attributes);

        int idx = 0;
        for (RecordAtts record : resolvedAtts) {

            List<Boolean> checkResult = new ArrayList<>();
            RecordElement element = new RecordElement(new RecordMeta(record));

            for (Predicate predicate : query.getPredicates()) {
                Predicate resolved = PredicateUtils.resolvePredicateWithAttributes(predicate, record.getAtts());
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

    @NotNull
    @Override
    public String getId() {
        return ID;
    }
}
