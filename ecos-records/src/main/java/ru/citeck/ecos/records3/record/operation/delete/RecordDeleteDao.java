package ru.citeck.ecos.records3.record.operation.delete;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

public interface RecordDeleteDao extends RecordsDao {

    @NotNull
    DelStatus delete(@NotNull String recordId);
}
