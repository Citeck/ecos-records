package ru.citeck.ecos.records3.record.op.delete.dao;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.dao.RecordsDao;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;

import java.util.List;

public interface RecordsDeleteDao extends RecordsDao {

    List<DelStatus> delete(@NotNull List<String> records);
}
