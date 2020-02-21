package ru.citeck.ecos.records2.source.common.group;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.AndPredicate;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;

import java.util.*;

public class RecordsGroupDAO extends LocalRecordsDAO implements LocalRecordsQueryWithMetaDAO {

    public static final String ID = "group";

    private static final int MAX_ITEMS_DEFAULT = 20;

    public RecordsGroupDAO() {
        setId(ID);
    }

    @Override
    public RecordsQueryResult queryLocalRecords(RecordsQuery query, MetaField field) {

        List<String> groupBy = query.getGroupBy();
        if (groupBy.isEmpty()) {
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

        String[] groupAtts = groupBy.get(0).split("&");
        int max = query.getMaxItems() > 0 ? query.getMaxItems() : MAX_ITEMS_DEFAULT;

        Predicate basePredicate = query.getQuery(Predicate.class);
        List<List<DistinctValue>> distinctValues = new ArrayList<>();

        for (String groupAtt : groupAtts) {

            List<DistinctValue> values = getDistinctValues(query.getSourceId(), basePredicate, groupAtt, max);

            if (values.isEmpty()) {
                return new RecordsQueryResult();
            }

            distinctValues.add(values);
        }

        RecordsQueryResult<RecordsGroup> result = new RecordsQueryResult<>();
        result.setRecords(getGroups(groupsBaseQuery, distinctValues, basePredicate, groupAtts));

        return result;
    }

    private List<RecordsGroup> getGroups(RecordsQuery groupsBaseQuery,
                                         List<List<DistinctValue>> distinctValues,
                                         Predicate basePredicate,
                                         String[] attributes) {

        List<RecordsGroup> groups = new ArrayList<>();

        if (distinctValues.size() == 1) {

            for (DistinctValue value : distinctValues.get(0)) {

                Map<String, DistinctValue> attributesMap = Collections.singletonMap(attributes[0], value);
                groups.add(createGroup(groupsBaseQuery, attributesMap, basePredicate));
            }
        } else {

            for (DistinctValue value0 : distinctValues.get(0)) {

                for (DistinctValue value1 : distinctValues.get(1)) {

                    Map<String, DistinctValue> attributesMap = new HashMap<>();
                    attributesMap.put(attributes[0], value0);
                    attributesMap.put(attributes[1], value1);

                    groups.add(createGroup(groupsBaseQuery, attributesMap, basePredicate));
                }
            }
        }

        return groups;
    }

    private RecordsGroup createGroup(RecordsQuery groupsBaseQuery,
                                     Map<String, DistinctValue> attributes,
                                     Predicate basePredicate) {

        RecordsQuery groupQuery = new RecordsQuery(groupsBaseQuery);

        AndPredicate groupPredicate = Predicates.and();
        groupPredicate.addPredicate(basePredicate);
        attributes.forEach((att, val) -> groupPredicate.addPredicate(Predicates.equal(att, val.getValue())));

        groupQuery.setQuery(groupPredicate);
        groupQuery.setLanguage(PredicateService.LANGUAGE_PREDICATE);

        return new RecordsGroup(groupQuery, attributes, groupPredicate, recordsService);
    }

    private List<DistinctValue> getDistinctValues(String sourceId, Predicate predicate, String attribute, int max) {

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setLanguage(DistinctQuery.LANGUAGE);

        DistinctQuery distinctQuery = new DistinctQuery();

        distinctQuery.setLanguage(PredicateService.LANGUAGE_PREDICATE);
        distinctQuery.setQuery(predicate);
        distinctQuery.setAttribute(attribute);

        recordsQuery.setMaxItems(max);
        recordsQuery.setSourceId(sourceId);
        recordsQuery.setQuery(distinctQuery);

        RecordsQueryResult<DistinctValue> values = recordsService.queryRecords(recordsQuery, DistinctValue.class);
        return values.getRecords();
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList(PredicateService.LANGUAGE_PREDICATE);
    }
}

