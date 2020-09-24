package ru.citeck.ecos.records3.source.dao.local.v2;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.op.query.request.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.request.query.RecsQueryRes;
import ru.citeck.ecos.records3.source.dao.RecordsQueryDao;

public interface LocalRecordsQueryDao extends RecordsQueryDao {

    RecsQueryRes<?> queryLocalRecords(@NotNull RecordsQuery query);
}
