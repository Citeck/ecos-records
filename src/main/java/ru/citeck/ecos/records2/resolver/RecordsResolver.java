package ru.citeck.ecos.records2.resolver;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.Collection;

public interface RecordsResolver {

    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema);

    RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema);

    RecordsMutResult mutate(RecordsMutation mutation);

    RecordsDelResult delete(RecordsDeletion deletion);
}
