package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDAO;

public interface RecordsQueryLocalDAO extends RecordsQueryDAO {

    RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery query);
}
