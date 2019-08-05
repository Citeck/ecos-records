package ru.citeck.ecos.records2.predicate;

import ru.citeck.ecos.predicate.Elements;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.List;
import java.util.stream.Collectors;

public class RecordElements implements Elements<RecordElement> {

    private List<RecordRef> recordRefs;
    private RecordsService recordsService;

    public RecordElements(RecordsService recordsService, List<RecordRef> recordRefs) {
        this.recordRefs = recordRefs;
        this.recordsService = recordsService;
    }

    @Override
    public Iterable<RecordElement> getElements(List<String> attributes) {

        RecordsResult<RecordMeta> recordsMeta = recordsService.getAttributes(recordRefs,
                                                                             RecordElement.getQueryAtts(attributes));
        return recordsMeta.getRecords()
                          .stream()
                          .map(RecordElement::new)
                          .collect(Collectors.toList());
    }
}

