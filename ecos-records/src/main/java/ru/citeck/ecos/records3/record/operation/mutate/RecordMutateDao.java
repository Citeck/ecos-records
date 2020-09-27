package ru.citeck.ecos.records3.record.operation.mutate;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

public interface RecordMutateDao extends RecordsDao {

    @NotNull
    RecordRef mutate(@NotNull RecordAtts record);
}
