package ru.citeck.ecos.records3.record.op.delete.dao;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.dao.RecordsDao;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;

public interface RecordDeleteDao extends RecordsDao {

    @NotNull
    DelStatus delete(@NotNull String recordId);
}
