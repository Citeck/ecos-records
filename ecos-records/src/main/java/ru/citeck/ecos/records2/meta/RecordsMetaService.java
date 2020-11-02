package ru.citeck.ecos.records2.meta;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Deprecated
public interface RecordsMetaService {

    Map<String, String> getAttributes(Class<?> metaClass);

    <T> T instantiateMeta(Class<T> metaClass, RecordMeta flatMeta);

    AttributesSchema createSchema(Map<String, String> attributes);

    List<RecordAtts> convertMetaResult(List<? extends RecordAtts> nodes, AttributesSchema schema, boolean flat);

    RecordAtts convertMetaResult(RecordAtts meta, AttributesSchema schema, boolean flat);
}
