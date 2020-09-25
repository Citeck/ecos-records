package ru.citeck.ecos.records3.record.operation.meta.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

import java.util.List;

public interface RecordsMetaDao extends RecordsDao {

    @Nullable
    List<?> getRecordsMeta(@NotNull List<String> records);
}
