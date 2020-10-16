package ru.citeck.ecos.records3.record.op.mutate.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records2.RecordRef;

public interface RecordMutateDtoDao<T> {

    @Nullable
    T getRecToMutate(@NotNull RecordRef recordRef);

    void saveMutatedRec(@NotNull T record);
}
