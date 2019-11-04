package ru.citeck.ecos.records2.source.dao.local.v2;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.RecordsQueryWithMetaDAO;

public interface LocalRecordsQueryWithMetaDAO<T> extends RecordsQueryWithMetaDAO {

    RecordsQueryResult<T> queryLocalRecords(RecordsQuery query, MetaField field);
}
