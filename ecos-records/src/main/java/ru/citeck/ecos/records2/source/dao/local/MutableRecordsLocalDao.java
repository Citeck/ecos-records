package ru.citeck.ecos.records2.source.dao.local;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.source.dao.MutableRecordsDao;

import java.util.List;

public interface MutableRecordsLocalDao<T> extends MutableRecordsDao {

    @NotNull
    List<T> getValuesToMutate(@NotNull List<RecordRef> records);

    @NotNull
    RecordsMutResult save(@NotNull List<T> values);
}
