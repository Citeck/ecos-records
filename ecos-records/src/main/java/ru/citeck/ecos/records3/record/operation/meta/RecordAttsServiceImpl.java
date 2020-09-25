package ru.citeck.ecos.records3.record.operation.meta;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.schema.AttsSchema;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaResolver;
import ru.citeck.ecos.records3.record.operation.meta.schema.resolver.AttSchemaResolver;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RecordAttsServiceImpl implements RecordAttsService {

    private static final String ID_ATT_ALIAS = "___id___";

    private final AttSchemaReader schemaReader;
    private final DtoSchemaResolver dtoSchemaResolver;
    private final AttSchemaResolver schemaResolver;

    public RecordAttsServiceImpl(RecordsServiceFactory factory) {
        this.dtoSchemaResolver = factory.getDtoMetaResolver();
        this.schemaReader = factory.getAttSchemaReader();
        this.schemaResolver = factory.getAttSchemaResolver();
    }

    @Override
    public RecordMeta getAtts(Object value, Map<String, String> attributes) {
        return getAtts(value, attributes, false);
    }

    @Override
    public RecordMeta getAtts(Object value, Collection<String> attributes) {
        return getAtts(value, attributes, false);
    }

    @Override
    public RecordMeta getAtts(Object value, Collection<String> attributes, boolean rawAtts) {
        return getAtts(value, toAttsMap(attributes), rawAtts);
    }

    @Override
    public RecordMeta getAtts(Object value, Map<String, String> attributes, boolean rawAtts) {
        List<Object> values = Collections.singletonList(value);
        return getFirst(getAtts(values, attributes, rawAtts), attributes, values);
    }

    @Override
    public <T> T getAtts(Object value, Class<T> attsDto) {
        List<Object> values = Collections.singletonList(value);
        return getFirst(getAtts(values, attsDto), attsDto, values);
    }

    @Override
    public List<RecordMeta> getAtts(List<Object> values, Collection<String> attributes) {
        return getAtts(values, attributes, false);
    }

    @Override
    public List<RecordMeta> getAtts(List<Object> values, Map<String, String> attributes) {
        return getAtts(values, attributes, false);
    }

    @Override
    public List<RecordMeta> getAtts(List<Object> values, Collection<String> attributes, boolean rawAtts) {
        return getAtts(values, toAttsMap(attributes), rawAtts);
    }

    @Override
    public <T> List<T> getAtts(List<Object> values, Class<T> attsDto) {
        Map<String, String> attributes = dtoSchemaResolver.getAttributes(attsDto);
        return values.stream()
            .map(v -> getAtts(v, attributes))
            .map(v -> dtoSchemaResolver.instantiateMeta(attsDto, v.getAttributes()))
            .collect(Collectors.toList());
    }

    @Override
    public List<RecordMeta> getAtts(List<Object> values, Map<String, String> attributes, boolean rawAtts) {
        AttsSchema schema = schemaReader.read(attributes);
        List<SchemaRootAtt> rootAtts = new ArrayList<>(schema.getAttributes());
        rootAtts.add(new SchemaRootAtt(
            new SchemaAtt(ID_ATT_ALIAS, ".id", false),
            Collections.emptyList(),
            Json.getMapper().getType(String.class))
        );
        List<ObjectData> data = schemaResolver.resolve(values, rootAtts, rawAtts);
        return data.stream()
            .map(this::toRecMeta)
            .collect(Collectors.toList());
    }

    private RecordMeta toRecMeta(ObjectData data) {
        String id = data.get(ID_ATT_ALIAS).asText();
        if (StringUtils.isBlank(id)) {
            id = UUID.randomUUID().toString();
        }
        data = data.deepCopy();
        data.remove(ID_ATT_ALIAS);
        return new RecordMeta(RecordRef.create("", id), data);
    }

    private <T> T getFirst(List<T> elements, Object atts, List<Object> srcValues) {
        if (elements.size() == 0) {
            throw new RuntimeException("Get atts returned 0 records. " +
                "Attributes: " + atts + " Values: " + srcValues);
        }
        return elements.get(0);
    }

    private Map<String, String> toAttsMap(Collection<String> attributes) {
        Map<String, String> attributesMap = new LinkedHashMap<>();
        attributes.forEach(att -> attributesMap.put(att, att));
        return attributesMap;
    }
}
