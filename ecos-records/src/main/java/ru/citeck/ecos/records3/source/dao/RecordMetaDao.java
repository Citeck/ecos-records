package ru.citeck.ecos.records3.source.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.RecordRef;

public interface RecordMetaDao extends RecordsDao {

    @Nullable
    Object getMeta(@NotNull RecordRef recordRef);
}
