package ru.citeck.ecos.records3.record.op.query.request.query.typed;

import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.op.query.request.query.RecsQueryRes;

import java.util.stream.Collectors;

/**
 * Used to deserialize query result with RecordRefs.
 */
public class RecsMetaQueryRes extends RecsQueryRes<RecordMeta> {

    public RecsMetaQueryRes() {
    }

    public RecsMetaQueryRes(RecsMetaQueryRes other) {
        super(other);
    }

    public RecsMetaQueryRes addSourceId(String sourceId) {
        for (RecordMeta record : getRecords()) {
            record.setId(RecordRef.create(sourceId, record.getId().toString()));
        }
        return this;
    }

    public RecsMetaQueryRes addAppName(String appName) {
        setRecords(getRecords().stream()
                               .map(m -> new RecordMeta(m, ref -> ref.addAppName(appName)))
                               .collect(Collectors.toList()));
        return this;
    }
}
