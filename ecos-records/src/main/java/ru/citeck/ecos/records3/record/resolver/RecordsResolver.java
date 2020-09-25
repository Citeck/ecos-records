package ru.citeck.ecos.records3.record.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;

import java.util.List;
import java.util.Map;

public interface RecordsResolver {

    @Nullable
    RecsQueryRes<RecordMeta> query(@NotNull RecordsQuery query,
                                   @NotNull Map<String, String> attributes,
                                   boolean rawAtts);

    @Nullable
    List<RecordMeta> getAtts(@NotNull List<RecordRef> records,
                             @NotNull Map<String, String> attributes,
                             boolean rawAtts);

    @Nullable
    RecordsMutResult mutate(@NotNull RecordsMutation mutation);

    @Nullable
    RecordsDelResult delete(@NotNull RecordsDeletion deletion);

    @Nullable
    RecsSourceInfo getSourceInfo(@NotNull String sourceId);

    @NotNull
    List<RecsSourceInfo> getSourceInfo();
}
