package ru.citeck.ecos.records3.record.op.mutate;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

import java.util.List;

public interface RecordsMutateDao extends RecordsDao {

    @NotNull
    List<RecordRef> mutate(@NotNull List<RecordMeta> records);
}
