package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

public interface RecordsQueryDao extends RecordsQueryBaseDao {

    RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query);
}
