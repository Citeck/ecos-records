package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;

/**
 * Query records with metadata.
 * @deprecated use interface from v2 package instead:
 *             ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao
 */
@Deprecated
public interface RecordsQueryWithMetaLocalDAO<T> extends LocalRecordsQueryWithMetaDAO<T> {

    RecordsQueryResult<T> getMetaValues(RecordsQuery query);

    @Override
    default RecordsQueryResult<T> queryLocalRecords(RecordsQuery query, MetaField field) {
        return getMetaValues(query);
    }
}
