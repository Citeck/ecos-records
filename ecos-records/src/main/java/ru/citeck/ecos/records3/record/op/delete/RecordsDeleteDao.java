package ru.citeck.ecos.records3.record.op.delete;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.dao.RecordsDao;

import java.util.List;

public interface RecordsDeleteDao extends RecordsDao {

    List<DelStatus> delete(@NotNull List<String> records);
}
