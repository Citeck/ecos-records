package ru.citeck.ecos.records3.record.operation.mutate;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;

import java.util.List;

public interface RecordsMutateDtoDao<T> {

    @NotNull
    List<T> getRecsToMutate(@NotNull List<RecordRef> recordRef);

    void saveMutatedRecs(@NotNull List<T> records);
}
