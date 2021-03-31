package ru.citeck.ecos.records2.source.dao;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;

/**
 * @deprecated -> Record(s)MutateDao + Record(s)DeleteDao
 */
@Deprecated
public interface MutableRecordsDao extends RecordsDao {

    RecordsMutResult mutate(@NotNull RecordsMutation mutation);

    RecordsDelResult delete(@NotNull RecordsDeletion deletion);
}
