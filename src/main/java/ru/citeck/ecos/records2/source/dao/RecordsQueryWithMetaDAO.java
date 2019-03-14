package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

public interface RecordsQueryWithMetaDAO extends RecordsQueryBaseDAO {

    /**
     * Query Records.
     *
     * @deprecated implement queryRecords instead
     */
    @Deprecated
    default RecordsQueryResult<RecordMeta> getRecords(RecordsQuery query, String metaSchema) {
        return new RecordsQueryResult<>();
    }

    default RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String metaSchema) {
        return getRecords(query, metaSchema);
    }
}
