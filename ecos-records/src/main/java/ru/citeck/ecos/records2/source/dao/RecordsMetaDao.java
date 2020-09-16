package ru.citeck.ecos.records2.source.dao;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.meta.schema.AttsSchema;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.List;

public interface RecordsMetaDao extends RecordsDao {

    RecordsResult<RecordMeta> getMeta(@NotNull List<RecordRef> records,
                                      @NotNull AttsSchema schema);
}
