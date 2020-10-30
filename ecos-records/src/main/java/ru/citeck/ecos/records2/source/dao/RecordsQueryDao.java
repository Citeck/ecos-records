package ru.citeck.ecos.records2.source.dao;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

@Deprecated
public interface RecordsQueryDao extends RecordsQueryBaseDao {

    @NotNull
    RecordsQueryResult<RecordRef> queryRecords(@NotNull RecordsQuery query);
}
