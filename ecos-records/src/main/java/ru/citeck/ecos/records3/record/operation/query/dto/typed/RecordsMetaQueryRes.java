package ru.citeck.ecos.records3.record.operation.query.dto.typed;

import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;

import java.util.stream.Collectors;

/**
 * Used to deserialize query result with RecordRefs.
 */
public class RecordsMetaQueryRes extends RecordsQueryRes<RecordAtts> {

    public RecordsMetaQueryRes() {
    }

    public RecordsMetaQueryRes(RecordsMetaQueryRes other) {
        super(other);
    }

    public RecordsMetaQueryRes addSourceId(String sourceId) {
        for (RecordAtts record : getRecords()) {
            record.setId(RecordRef.create(sourceId, record.getId().toString()));
        }
        return this;
    }

    public RecordsMetaQueryRes addAppName(String appName) {
        setRecords(getRecords().stream()
                               .map(m -> new RecordAtts(m, ref -> ref.addAppName(appName)))
                               .collect(Collectors.toList()));
        return this;
    }
}
