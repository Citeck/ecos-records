package ru.citeck.ecos.records2.request.query.typed;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.stream.Collectors;

/**
 * Used to deserialize query result with RecordRefs.
 */
public class RecordsMetaQueryResult extends RecordsQueryResult<RecordMeta> {

    public RecordsMetaQueryResult() {
    }

    public RecordsMetaQueryResult(RecordsMetaQueryResult other) {
        super(other);
    }

    public RecordsMetaQueryResult addSourceId(String sourceId) {
        for (RecordAtts record : getRecords()) {
            record.setId(EntityRef.create(sourceId, record.getId().toString()));
        }
        return this;
    }

    public RecordsMetaQueryResult addAppName(String appName) {
        setRecords(getRecords().stream()
                               .map(m -> new RecordMeta(m, ref -> ref.withAppName(appName)))
                               .collect(Collectors.toList()));
        return this;
    }
}
