package ru.citeck.ecos.records3.record.op.atts;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.record.dao.RecordsDao;

import java.util.List;

public interface RecordsAttsDao extends RecordsDao {

    @Nullable
    List<?> getRecordsAtts(@NotNull List<String> records);
}
