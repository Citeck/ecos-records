package ru.citeck.ecos.records2.source.dao.local;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.source.dao.MutableRecordsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;

/**
 * @deprecated replace with RecordMutateDtoDao
 */
@Deprecated
public interface MutableRecordsLocalDao<T> extends MutableRecordsDao {

    @NotNull
    List<T> getValuesToMutate(@NotNull List<EntityRef> records);

    @NotNull
    RecordsMutResult save(@NotNull List<T> values);
}
