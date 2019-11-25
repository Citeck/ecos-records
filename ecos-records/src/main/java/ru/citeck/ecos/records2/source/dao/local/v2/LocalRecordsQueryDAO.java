package ru.citeck.ecos.records2.source.dao.local.v2;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDAO;

public interface LocalRecordsQueryDAO extends RecordsQueryDAO {

    RecordsQueryResult<RecordRef> queryLocalRecords(RecordsQuery query);
}
