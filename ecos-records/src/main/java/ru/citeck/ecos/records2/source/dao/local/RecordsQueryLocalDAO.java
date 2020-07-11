package ru.citeck.ecos.records2.source.dao.local;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDAO;

/**
 * Interface to get records by query.
 * @deprecated use interface from v2 package instead:
 *             ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDao
 */
@Deprecated
public interface RecordsQueryLocalDAO extends LocalRecordsQueryDAO {

    RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery query);

    @NotNull
    @Override
    default RecordsQueryResult<RecordRef> queryLocalRecords(@NotNull RecordsQuery query) {
        return getLocalRecords(query);
    }
}
