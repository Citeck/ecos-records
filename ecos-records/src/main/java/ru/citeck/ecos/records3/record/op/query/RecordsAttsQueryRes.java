package ru.citeck.ecos.records3.record.op.query;

import ru.citeck.ecos.records3.record.op.atts.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;

import java.util.stream.Collectors;

/**
 * Used to deserialize query result with RecordAtts.
 */
public class RecordsAttsQueryRes extends RecordsQueryRes<RecordAtts> {

    public RecordsAttsQueryRes() {
    }

    public RecordsAttsQueryRes(RecordsAttsQueryRes other) {
        super(other);
    }

    public RecordsAttsQueryRes addSourceId(String sourceId) {
        for (RecordAtts record : getRecords()) {
            record.setId(RecordRef.create(sourceId, record.getId().toString()));
        }
        return this;
    }

    public RecordsAttsQueryRes addAppName(String appName) {
        setRecords(getRecords().stream()
                               .map(m -> new RecordAtts(m, ref -> ref.addAppName(appName)))
                               .collect(Collectors.toList()));
        return this;
    }
}
