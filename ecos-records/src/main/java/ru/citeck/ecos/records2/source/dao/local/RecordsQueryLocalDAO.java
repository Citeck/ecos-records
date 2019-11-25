package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDAO;

/**
 * Interface to get records by query.
 * @deprecated use interface from v2 package instead:
 *             ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDAO
 */
@Deprecated
public interface RecordsQueryLocalDAO extends LocalRecordsQueryDAO {

    RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery query);

    @Override
    default RecordsQueryResult<RecordRef> queryLocalRecords(RecordsQuery query) {
        return getLocalRecords(query);
    }
}
