package ru.citeck.ecos.records3.source.dao.local.v2;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.RecordsQueryDao;

public interface LocalRecordsQueryDao extends RecordsQueryDao {

    RecordsQueryRes<?> queryLocalRecords(@NotNull RecordsQuery query);
}
