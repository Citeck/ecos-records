package ru.citeck.ecos.records3.record.operation.meta.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

public interface RecordMetaDao extends RecordsDao {

    @Nullable
    Object getRecordMeta(@NotNull String recordId);
}
