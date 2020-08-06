package ru.citeck.ecos.records2.source.dao.local.v2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.RecordsMetaDao;

import java.util.List;

public interface LocalRecordsMetaDao extends RecordsMetaDao {

    @Nullable
    List<?> getLocalRecordsMeta(@NotNull List<RecordRef> records,
                                @NotNull MetaField metaField);
}
