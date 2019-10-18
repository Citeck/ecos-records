package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.List;

public interface RecordsMetaDAO extends RecordsDAO {

    RecordsResult<RecordMeta> getMeta(List<RecordRef> records, String gqlSchema);
}
