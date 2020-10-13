package ru.citeck.ecos.records2.predicate;

import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records2.IterableRecords;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordElements implements Elements<RecordElement> {

    private List<RecordRef> recordRefs;
    private final RecordsService recordsService;
    private RecordsQuery recordsQuery;
    private Function<RecordRef, RecordRef> refsMapping;

    public RecordElements(RecordsService recordsService, List<RecordRef> recordRefs) {

        MandatoryParam.check("recordRefs", recordRefs);
        MandatoryParam.check("recordsService", recordsService);

        this.recordRefs = recordRefs;
        this.recordsService = recordsService;
    }

    public RecordElements(RecordsService recordsService, RecordsQuery recordsQuery) {
        this(recordsService, recordsQuery, null);
    }

    public RecordElements(RecordsService recordsService, RecordsQuery recordsQuery,
                          Function<RecordRef, RecordRef> refsMapping) {

        MandatoryParam.check("recordsQuery", recordsQuery);
        MandatoryParam.check("recordsService", recordsService);

        this.recordsQuery = recordsQuery;
        this.recordsService = recordsService;
        this.refsMapping = refsMapping != null ? refsMapping : r -> r;
    }

    @Override
    public Iterable<RecordElement> getElements(List<String> attributes) {
        if (recordRefs != null) {
            return getElements(recordRefs, attributes);
        } else {
            return new RecordsByQuery(recordsQuery, RecordElement.getQueryAtts(attributes));
        }
    }

    private List<RecordElement> getElements(List<RecordRef> refs, List<String> attributes) {
        return getElements(refs, RecordElement.getQueryAtts(attributes));
    }

    private List<RecordElement> getElements(List<RecordRef> refs, Map<String, String> attributes) {

        RecordsResult<RecordMeta> recordsMeta = recordsService.getAttributes(refs, attributes);

        return recordsMeta.getRecords()
            .stream()
            .map(RecordElement::new)
            .collect(Collectors.toList());
    }

    private class RecordsByQuery implements Iterable<RecordElement> {

        private final IterableRecords records;
        private final Map<String, String> attributes;

        RecordsByQuery(RecordsQuery query, Map<String, String> attributes) {
            this.attributes = attributes;
            this.records = new IterableRecords(recordsService, query);
        }

        @Override
        public Iterator<RecordElement> iterator() {
            return new RecordsIterator(records.iterator(), attributes);
        }
    }

    private class RecordsIterator implements Iterator<RecordElement> {

        private List<RecordElement> records = null;

        private final Iterator<RecordRef> refsIterator;
        private final Map<String, String> attributes;

        private int idx = -1;

        RecordsIterator(Iterator<RecordRef> refsIterator, Map<String, String> attributes) {
            this.refsIterator = refsIterator;
            this.attributes = attributes;
        }

        private void updateRecords() {

            this.idx = 0;
            int iteratorIdx = 0;

            List<RecordRef> recordRefs = new ArrayList<>();

            while (refsIterator.hasNext()) {
                recordRefs.add(refsMapping.apply(refsIterator.next()));
                if (++iteratorIdx >= 100) {
                    break;
                }
            }

            if (recordRefs.isEmpty()) {
                records = Collections.emptyList();
            } else {
                records = getElements(recordRefs, attributes);
            }
        }

        @Override
        public boolean hasNext() {
            if (records == null || idx > 0 && idx >= records.size()) {
                updateRecords();
            }
            return idx < records.size();
        }

        @Override
        public RecordElement next() {
            return records.get(idx++);
        }
    }
}

