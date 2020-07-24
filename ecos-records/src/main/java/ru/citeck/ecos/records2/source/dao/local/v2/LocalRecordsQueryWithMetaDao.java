package ru.citeck.ecos.records2.source.dao.local.v2;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.RecordsQueryWithMetaDao;

public interface LocalRecordsQueryWithMetaDao<T> extends RecordsQueryWithMetaDao {

    RecordsQueryResult<T> queryLocalRecords(@NotNull RecordsQuery recordsQuery, @NotNull MetaField field);
}
