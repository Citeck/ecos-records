package ru.citeck.ecos.records3.record.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.delete.DelStatus;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;

import java.util.List;
import java.util.Map;

public interface RecordsResolver {

    @Nullable
    RecordsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                      @NotNull Map<String, String> attributes,
                                      boolean rawAtts);

    @Nullable
    List<RecordAtts> getAtts(@NotNull List<RecordRef> records,
                             @NotNull Map<String, String> attributes,
                             boolean rawAtts);

    @Nullable
    List<RecordRef> mutate(@NotNull List<RecordAtts> records);

    @Nullable
    List<DelStatus> delete(@NotNull List<RecordRef> records);

    @Nullable
    RecsSourceInfo getSourceInfo(@NotNull String sourceId);

    @NotNull
    List<RecsSourceInfo> getSourceInfo();
}
