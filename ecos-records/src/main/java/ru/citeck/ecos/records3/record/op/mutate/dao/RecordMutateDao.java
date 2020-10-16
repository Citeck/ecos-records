package ru.citeck.ecos.records3.record.op.mutate.dao;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.dao.RecordsDao;

public interface RecordMutateDao extends RecordsDao {

    @NotNull
    RecordRef mutate(@NotNull RecordAtts record);
}
