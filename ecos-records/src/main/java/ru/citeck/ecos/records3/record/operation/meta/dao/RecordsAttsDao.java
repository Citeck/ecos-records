package ru.citeck.ecos.records3.record.operation.meta.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

import java.util.List;

public interface RecordsAttsDao extends RecordsDao {

    @Nullable
    List<?> getRecordsAtts(@NotNull List<String> records);
}
