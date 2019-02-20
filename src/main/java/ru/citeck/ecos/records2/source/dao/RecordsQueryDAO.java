package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

public interface RecordsQueryDAO extends RecordsDAO {

    RecordsQueryResult<RecordRef> getRecords(RecordsQuery query);

}
