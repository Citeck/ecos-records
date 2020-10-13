package ru.citeck.ecos.records3.record.op.mutate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.record.op.atts.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.dao.RecordsDao;

import java.util.List;

public interface RecordsMutateDao extends RecordsDao {

    @Nullable
    List<RecordRef> mutate(@NotNull List<RecordAtts> records);
}
