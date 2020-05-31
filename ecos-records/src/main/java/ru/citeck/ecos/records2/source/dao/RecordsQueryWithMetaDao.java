package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

public interface RecordsQueryWithMetaDao extends RecordsQueryBaseDao {

    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String metaSchema);
}
