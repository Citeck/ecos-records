package ru.citeck.ecos.records3.record.operation.delete;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

import java.util.List;

public interface RecordsDeleteDao extends RecordsDao {

    List<DelStatus> delete(@NotNull List<String> records);
}
