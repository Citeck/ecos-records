package ru.citeck.ecos.records3.record.operation.meta;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.schema.AttSchema;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaResolver;
import ru.citeck.ecos.records3.record.operation.meta.schema.resolver.AttResolver;
import ru.citeck.ecos.records3.record.operation.meta.schema.resolver.ResolveArgs;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RecordAttsServiceImpl implements RecordAttsService {

    private static final String REF_ATT_ALIAS = "___ref___";

    private final AttSchemaReader schemaReader;
    private final DtoSchemaResolver dtoSchemaResolver;
    private final AttResolver schemaResolver;

    public RecordAttsServiceImpl(RecordsServiceFactory factory) {
        this.dtoSchemaResolver = factory.getDtoMetaResolver();
        this.schemaReader = factory.getAttSchemaReader();
        this.schemaResolver = factory.getAttSchemaResolver();
    }

    @Override
    public RecordAtts getAtts(Object value, Map<String, String> attributes) {
        return getAtts(value, attributes, false);
    }

    @Override
    public RecordAtts getAtts(Object value, Collection<String> attributes) {
        return getAtts(value, attributes, false);
    }

    @Override
    public RecordAtts getAtts(Object value, Collection<String> attributes, boolean rawAtts) {
        return getAtts(value, toAttsMap(attributes), rawAtts);
    }

    @Override
    public RecordAtts getAtts(Object value, Map<String, String> attributes, boolean rawAtts) {
        List<Object> values = Collections.singletonList(value);
        return getFirst(getAtts(values, attributes, rawAtts), attributes, values);
    }

    @Override
    public <T> T getAtts(Object value, Class<T> attsDto) {
        List<Object> values = Collections.singletonList(value);
        return getFirst(getAtts(values, attsDto), attsDto, values);
    }

    @Override
    public List<RecordAtts> getAtts(List<Object> values, Collection<String> attributes) {
        return getAtts(values, attributes, false);
    }

    @Override
    public List<RecordAtts> getAtts(List<Object> values, Map<String, String> attributes) {
        return getAtts(values, attributes, false);
    }

    @Override
    public List<RecordAtts> getAtts(List<Object> values, Collection<String> attributes, boolean rawAtts) {
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
    public List<RecordAtts> getAtts(List<Object> values, Map<String, String> attributes, boolean rawAtts) {

        AttSchema schema = schemaReader.read(attributes);

        List<SchemaRootAtt> rootAtts = new ArrayList<>(schema.getAttributes());

        rootAtts.add(new SchemaRootAtt(
            SchemaAtt.create()
                .setAlias(REF_ATT_ALIAS)
                .setName("?ref")
                .build(),
            Collections.emptyList())
        );

        List<ObjectData> data = schemaResolver.resolve(ResolveArgs.create()
            .setValues(values)
            .setAtts(rootAtts)
            .setRawAtts(rawAtts)
            .build());

        return data.stream()
            .map(this::toRecMeta)
            .collect(Collectors.toList());
    }

    private RecordAtts toRecMeta(ObjectData data) {
        String id = data.get(REF_ATT_ALIAS).asText();
        if (StringUtils.isBlank(id)) {
            id = UUID.randomUUID().toString();
        }
        data = data.deepCopy();
        data.remove(REF_ATT_ALIAS);
        return new RecordAtts(RecordRef.create("", id), data);
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
