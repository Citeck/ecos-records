package ru.citeck.ecos.records2.source.dao.local.v2;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDao;

public interface LocalRecordsQueryDao extends RecordsQueryDao {

    RecordsQueryResult<?> queryLocalRecords(@NotNull RecordsQuery query);
}
