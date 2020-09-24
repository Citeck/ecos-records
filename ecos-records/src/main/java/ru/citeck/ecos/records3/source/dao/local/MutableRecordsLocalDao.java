package ru.citeck.ecos.records3.source.dao.local;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutResult;
import ru.citeck.ecos.records3.record.operation.mutate.RecordsMutateDao;

import java.util.List;

public interface MutableRecordsLocalDao<T> extends RecordsMutateDao {

    @NotNull
    List<T> getValuesToMutate(@NotNull List<RecordRef> records);

    @NotNull
    RecordsMutResult save(@NotNull List<T> values);
}
