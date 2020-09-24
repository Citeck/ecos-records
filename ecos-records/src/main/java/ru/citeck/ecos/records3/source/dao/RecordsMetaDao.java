package ru.citeck.ecos.records3.source.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.RecordRef;

import java.util.List;

public interface RecordsMetaDao extends RecordsDao {

    @Nullable
    List<?> getRecordsMeta(@NotNull List<RecordRef> records);
}
