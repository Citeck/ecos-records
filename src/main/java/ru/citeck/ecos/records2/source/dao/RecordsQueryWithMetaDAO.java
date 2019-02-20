package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

public interface RecordsQueryWithMetaDAO extends RecordsDAO {

    RecordsQueryResult<RecordMeta> getRecords(RecordsQuery query, String metaSchema);
}
