package ru.citeck.ecos.records3.record.op.atts.service;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttSchemaResolver;
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.ResolveArgs;
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RecordAttsServiceImpl implements RecordAttsService {

    private static final String REF_ATT_ALIAS = "___ref___";

    private final AttSchemaReader schemaReader;
    private final DtoSchemaReader dtoSchemaReader;
    private final AttSchemaResolver schemaResolver;

    public RecordAttsServiceImpl(RecordsServiceFactory factory) {
        this.dtoSchemaReader = factory.getDtoSchemaReader();
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
    public List<RecordAtts> getAtts(List<?> values, Collection<String> attributes) {
        return getAtts(values, attributes, false);
    }

    @Override
    public List<RecordAtts> getAtts(List<?> values, Map<String, String> attributes) {
        return getAtts(values, attributes, false);
    }

    @Override
    public List<RecordAtts> getAtts(List<?> values, Collection<String> attributes, boolean rawAtts) {
        return getAtts(values, toAttsMap(attributes), rawAtts);
    }

    @Override
    public <T> List<T> getAtts(List<?> values, Class<T> attsDto) {

        List<SchemaAtt> attributes = dtoSchemaReader.read(attsDto);

        return getAtts(values, attributes, false, Collections.emptyList())
            .stream()
            .map(v -> dtoSchemaReader.instantiate(attsDto, v.getAtts()))
            .collect(Collectors.toList());
    }

    @Override
    public List<RecordAtts> getAtts(List<?> values, Map<String, String> attributes, boolean rawAtts) {
        return getAtts(values, attributes, rawAtts, Collections.emptyList());
    }

    @Override
    public List<RecordAtts> getAtts(List<?> values,
                                    List<SchemaAtt> rootAtts,
                                    boolean rawAtts,
                                    List<AttMixin> mixins) {

        return getAtts(values, rootAtts, rawAtts, mixins, Collections.emptyList());
    }

    @Override
    public List<RecordAtts> getAtts(List<?> values,
                                    List<SchemaAtt> rootAtts,
                                    boolean rawAtts,
                                    List<AttMixin> mixins,
                                    List<RecordRef> valuesRefs) {

        boolean valueRefsProvided = valuesRefs.size() == values.size();

        rootAtts = new ArrayList<>(rootAtts);
        if (!valueRefsProvided) {
            rootAtts.add(SchemaAtt.create()
                    .withAlias(REF_ATT_ALIAS)
                    .withName("?id")
                    .build());
        }

        List<Map<String, Object>> data = schemaResolver.resolve(ResolveArgs.create()
            .setValues(values)
            .setAtts(rootAtts)
            .setRawAtts(rawAtts)
            .setMixins(mixins)
            .setValueRefs(valuesRefs)
            .build());

        List<RecordAtts> recordAtts = new ArrayList<>();
        if (valueRefsProvided) {
            for (int i = 0; i < data.size(); i++) {
                recordAtts.add(toRecAtts(data.get(i), valuesRefs.get(i)));
            }
        } else {
            for (Map<String, Object> elem : data) {
                recordAtts.add(toRecAtts(elem, RecordRef.EMPTY));
            }
        }
        return recordAtts;
    }

    @Override
    public List<RecordAtts> getAtts(List<?> values,
                                    Map<String, String> attributes,
                                    boolean rawAtts,
                                    List<AttMixin> mixins) {

        return getAtts(values, schemaReader.read(attributes), rawAtts, mixins);
    }

    private RecordAtts toRecAtts(Map<String, Object> data, RecordRef id) {
        if (id == RecordRef.EMPTY) {
            Object alias = data.get(REF_ATT_ALIAS);
            id = alias == null ? RecordRef.EMPTY : RecordRef.valueOf(alias.toString());
            if (StringUtils.isBlank(id.getId())) {
                id = RecordRef.create(id.getAppName(), id.getSourceId(), UUID.randomUUID().toString());
            }
            data = new LinkedHashMap<>(data);
            data.remove(REF_ATT_ALIAS);
        }
        return new RecordAtts(id, ObjectData.create(data));
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
