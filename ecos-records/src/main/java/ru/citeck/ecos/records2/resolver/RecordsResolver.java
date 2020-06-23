package ru.citeck.ecos.records2.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.info.RecordsSourceInfo;

import java.util.Collection;
import java.util.List;

public interface RecordsResolver {

    @NotNull
    RecordsQueryResult<RecordMeta> queryRecords(@NotNull RecordsQuery query, String schema);

    @NotNull
    RecordsResult<RecordMeta> getMeta(@NotNull Collection<RecordRef> records, String schema);

    @NotNull
    RecordsMutResult mutate(@NotNull RecordsMutation mutation);

    @NotNull
    RecordsDelResult delete(@NotNull RecordsDeletion deletion);

    @Nullable
    RecordsSourceInfo getSourceInfo(@NotNull String sourceId);

    @NotNull
    List<RecordsSourceInfo> getSourceInfo();
}
