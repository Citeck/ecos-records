package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

public interface RecordsQueryDAO extends RecordsQueryBaseDAO {

    /**
     * @deprecated implement queryRecords instead
     */
    @Deprecated
    default RecordsQueryResult<RecordRef> getRecords(RecordsQuery query) {
        return new RecordsQueryResult<>();
    }

    default RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {
        return getRecords(query);
    }
}
