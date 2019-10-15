package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.RecordsQueryWithMetaDAO;

public interface RecordsQueryWithMetaLocalDAO<T> extends RecordsQueryWithMetaDAO {

    RecordsQueryResult<T> getMetaValues(RecordsQuery query);
}
