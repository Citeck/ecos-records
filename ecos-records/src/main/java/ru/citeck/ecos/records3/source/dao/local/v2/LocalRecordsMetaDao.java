package ru.citeck.ecos.records3.source.dao.local.v2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.source.dao.RecordsMetaDao;

import java.util.List;

public interface LocalRecordsMetaDao extends RecordsMetaDao {

    @Nullable
    List<?> getLocalRecordsMeta(@NotNull List<RecordRef> records);
}
