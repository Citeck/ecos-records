package ru.citeck.ecos.records3.record.op.delete;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.dao.RecordsDao;

public interface RecordDeleteDao extends RecordsDao {

    @NotNull
    DelStatus delete(@NotNull String recordId);
}
