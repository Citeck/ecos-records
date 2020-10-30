package ru.citeck.ecos.records2.request.query.typed;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;

import java.util.stream.Collectors;

/**
 * Used to deserialize query result with RecordRefs.
 */
public class RecordsMetaQueryResult extends RecordsQueryResult<RecordAtts> {

    public RecordsMetaQueryResult() {
    }

    public RecordsMetaQueryResult(RecordsMetaQueryResult other) {
        super(other);
    }

    public RecordsMetaQueryResult addSourceId(String sourceId) {
        for (RecordAtts record : getRecords()) {
            record.setId(RecordRef.create(sourceId, record.getId().toString()));
        }
        return this;
    }

    public RecordsMetaQueryResult addAppName(String appName) {
        setRecords(getRecords().stream()
                               .map(m -> new RecordAtts(m, ref -> ref.addAppName(appName)))
                               .collect(Collectors.toList()));
        return this;
    }
}
