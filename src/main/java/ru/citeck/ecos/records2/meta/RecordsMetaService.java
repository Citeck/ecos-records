package ru.citeck.ecos.records2.meta;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.List;
import java.util.Map;

public interface RecordsMetaService {

    Map<String, String> getAttributes(Class<?> metaClass);

    <T> T instantiateMeta(Class<T> metaClass, RecordMeta flatMeta);

    AttributesSchema createSchema(Map<String, String> attributes);

    List<RecordMeta> convertToFlatMeta(List<RecordMeta> nodes, AttributesSchema schema);

    RecordsResult<RecordMeta> getMeta(List<?> records, String schema);

}
