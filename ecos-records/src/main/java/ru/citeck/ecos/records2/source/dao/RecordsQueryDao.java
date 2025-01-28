package ru.citeck.ecos.records2.source.dao;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

/**
 * @deprecated -> RecordsQueryDao from records3 package
 */
@Deprecated
public interface RecordsQueryDao extends RecordsQueryBaseDao {

    @NotNull
    RecordsQueryResult<EntityRef> queryRecords(@NotNull RecordsQuery query);
}
