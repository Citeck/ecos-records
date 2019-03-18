package ru.citeck.ecos.records2.source.common.group;

import ru.citeck.ecos.predicate.model.*;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;

import java.util.*;
import java.util.stream.Collectors;

public class RecordsGroupDAO extends LocalRecordsDAO implements RecordsQueryWithMetaLocalDAO {

    public static final String ID = "group";

    private static final int MAX_ITEMS_DEFAULT = 20;

    public RecordsGroupDAO() {
        setId(ID);
    }

    @Override
    public RecordsQueryResult getMetaValues(RecordsQuery query) {

        List<String> groupBy = query.getGroupBy();
        if (groupBy.isEmpty()) {
            return new RecordsQueryResult();
        }

        String attribute = groupBy.get(0);
        int max = query.getMaxItems() > 0 ? query.getMaxItems() : MAX_ITEMS_DEFAULT;

        Predicate predicate = predicateService.readJson(query.getQuery());
        List<String> distinctValues = getDistinctValues(query.getSourceId(), predicate, attribute, max);

        if (distinctValues.isEmpty()) {
            return new RecordsQueryResult();
        }

        RecordsQuery groupsBaseQuery = new RecordsQuery(query);
        if (groupBy.size() == 1) {
            groupsBaseQuery.setGroupBy(null);
        } else {
            List<String> newGroupBy = new ArrayList<>();
            for (int i = 1; i < groupBy.size(); i++) {
                newGroupBy.add(groupBy.get(i));
            }
            groupsBaseQuery.setGroupBy(newGroupBy);
        }

        List<RecordsGroup> groups = new ArrayList<>();

        for (String value : distinctValues) {

            Predicate groupPredicate = Predicates.equal(attribute, value);
            RecordsQuery groupQuery = new RecordsQuery(groupsBaseQuery);

            groupQuery.setQuery(predicateService.writeJson(AndPredicate.of(predicate, groupPredicate)));
            groupQuery.setLanguage(RecordsService.LANGUAGE_PREDICATE);

            groups.add(new RecordsGroup(groupQuery, groupPredicate, recordsService));
        }

        RecordsQueryResult<RecordsGroup> result = new RecordsQueryResult<>();
        result.setRecords(groups);

        return result;
    }

    private List<String> getDistinctValues(String sourceId, Predicate predicate, String attribute, int max) {

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setLanguage(RecordsService.LANGUAGE_PREDICATE);
        recordsQuery.setSourceId(sourceId);
        recordsQuery.setMaxItems(max);

        OrPredicate distinctPredicate = Predicates.or(Predicates.empty(attribute));
        AndPredicate fullPredicate = Predicates.and(predicate, Predicates.not(distinctPredicate));

        Set<String> values = new HashSet<>();

        int found;
        int requests = 0;

        do {

            recordsQuery.setQuery(predicateService.writeJson(fullPredicate));
            Set<String> newValues = getValues(recordsQuery, attribute);
            found = newValues.size();

            for (String value : newValues) {
                distinctPredicate.addPredicate(Predicates.equal(attribute, value));
            }

            values.addAll(newValues);

        } while (found > 0 && values.size() <= max && ++requests <= max);

        return new ArrayList<>(values);

    }

    private Set<String> getValues(RecordsQuery query, String attribute) {

        RecordsQueryResult<RecordMeta> result =
                recordsService.queryRecords(query, Collections.singletonMap("att", attribute + "?str"));

        return result.getRecords()
                     .stream()
                     .map(r -> r.get("att", ""))
                     .filter(v -> v.length() > 0)
                     .collect(Collectors.toSet());
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList(RecordsService.LANGUAGE_PREDICATE);
    }
}

