package ru.citeck.ecos.records3.record.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.op.delete.request.RecordsDelResult;
import ru.citeck.ecos.records3.record.op.delete.request.RecordsDeletion;
import ru.citeck.ecos.records3.record.op.mutate.request.RecordsMutResult;
import ru.citeck.ecos.records3.record.op.mutate.request.RecordsMutation;
import ru.citeck.ecos.records3.record.op.query.request.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.request.query.RecsQueryRes;
import ru.citeck.ecos.records3.source.info.RecordsSourceInfo;

import java.util.List;
import java.util.Map;

public interface RecordsResolver {

    @Nullable
    RecsQueryRes<RecordMeta> queryRecords(@NotNull RecordsQuery query,
                                          @NotNull Map<String, String> attributes,
                                          boolean rawAtts);

    @Nullable
    List<RecordMeta> getMeta(@NotNull List<RecordRef> records,
                             @NotNull Map<String, String> attributes,
                             boolean rawAtts);

    @Nullable
    RecordsMutResult mutate(@NotNull RecordsMutation mutation);

    @Nullable
    RecordsDelResult delete(@NotNull RecordsDeletion deletion);

    @Nullable
    RecordsSourceInfo getSourceInfo(@NotNull String sourceId);

    @NotNull
    List<RecordsSourceInfo> getSourceInfo();
}
