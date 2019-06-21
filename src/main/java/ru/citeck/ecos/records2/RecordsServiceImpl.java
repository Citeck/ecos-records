package ru.citeck.ecos.records2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.meta.AttributesSchema;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.meta.RecordsMetaServiceAware;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.resolver.RecordsDAORegistry;
import ru.citeck.ecos.records2.resolver.RecordsResolver;
import ru.citeck.ecos.records2.source.dao.*;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecordsServiceImpl extends AbstractRecordsService {

    private static final Pattern ATT_PATTERN = Pattern.compile("^\\.atts?\\(n:\"([^\"]+)\"\\).+");

    private static final Log logger = LogFactory.getLog(RecordsServiceImpl.class);

    private RecordsMetaService recordsMetaService;
    private RecordsResolver recordsResolver;

    public RecordsServiceImpl(RecordsMetaService recordsMetaService, RecordsResolver recordsResolver) {

        this.recordsMetaService = recordsMetaService;
        this.recordsResolver = recordsResolver;

        if (this.recordsResolver instanceof RecordsServiceAware) {
            ((RecordsServiceAware) recordsResolver).setRecordsService(this);
        }
        if (this.recordsResolver instanceof RecordsMetaServiceAware) {
            ((RecordsMetaServiceAware) recordsResolver).setRecordsMetaService(recordsMetaService);
        }
        if (this.recordsMetaService instanceof RecordsServiceAware) {
            ((RecordsServiceAware) recordsMetaService).setRecordsService(this);
        }
    }

    /* QUERY */

    @Override
    public RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {
        return new RecordsQueryResult<>(recordsResolver.queryRecords(query, ""), RecordMeta::getId);
    }

    @Override
    public <T> RecordsQueryResult<T> queryRecords(RecordsQuery query, Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Meta class doesn't has any fields with setter. Class: " + metaClass);
        }

        RecordsQueryResult<RecordMeta> meta = queryRecords(query, attributes);

        return new RecordsQueryResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, toAttributesMap(attributes));
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes) {

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsQueryResult<RecordMeta> records = queryRecords(query, schema.getSchema());
        records.setRecords(recordsMetaService.convertToFlatMeta(records.getRecords(), schema));

        return records;
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {
        return recordsResolver.queryRecords(query, schema);
    }

    /* ATTRIBUTES */

    @Override
    public JsonNode getAttribute(RecordRef record, String attribute) {
        RecordsResult<RecordMeta> meta = getAttributes(Collections.singletonList(record),
                                                       Collections.singletonList(attribute));
        if (!meta.getRecords().isEmpty()) {
            return meta.getRecords().get(0).getAttribute(attribute);
        }
        return MissingNode.getInstance();
    }

    @Override
    public RecordMeta getAttributes(RecordRef record, Collection<String> attributes) {

        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordMeta getAttributes(RecordRef record, Map<String, String> attributes) {

        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Collection<String> attributes) {

        return getAttributes(records, toAttributesMap(attributes));
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Map<String, String> attributes) {

        return getAttributesImpl(records, attributes, true);
    }

    @Override
    public RecordMeta getRawAttributes(RecordRef record, Map<String, String> attributes) {
        return extractOne(getRawAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordsResult<RecordMeta> getRawAttributes(Collection<RecordRef> records, Map<String, String> attributes) {
        return getAttributesImpl(records, attributes, false);
    }

    private RecordsResult<RecordMeta> getAttributesImpl(Collection<RecordRef> records,
                                                        Map<String, String> attributes,
                                                        boolean flatAttributes) {

        if (attributes.isEmpty()) {
            return new RecordsResult<>(new ArrayList<>(records), RecordMeta::new);
        }

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsResult<RecordMeta> meta = getMeta(records, schema.getSchema());
        if (flatAttributes) {
            meta.setRecords(recordsMetaService.convertToFlatMeta(meta.getRecords(), schema));
        }

        return meta;
    }

    /* META */

    @Override
    public <T> T getMeta(RecordRef recordRef, Class<T> metaClass) {

        RecordsResult<T> meta = getMeta(Collections.singletonList(recordRef), metaClass);
        if (meta.getRecords().size() == 0) {
            throw new IllegalStateException("Can't get record metadata. Result: " + meta);
        }
        return meta.getRecords().get(0);
    }

    @Override
    public <T> RecordsResult<T> getMeta(Collection<RecordRef> records, Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            logger.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        RecordsResult<RecordMeta> meta = getAttributes(records, attributes);

        return new RecordsResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {
        return recordsResolver.getMeta(records, schema);
    }

    /* MODIFICATION */

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        Map<String, RecordRef> aliasToRecordRef = new HashMap<>();
        RecordsMutResult result = new RecordsMutResult();

        List<RecordMeta> records = mutation.getRecords();

        for (int i = records.size() - 1; i >= 0; i--) {

            RecordMeta record = records.get(i);

            ObjectNode attributes = JsonNodeFactory.instance.objectNode();

            record.forEach((name, value) -> {

                String simpleName = name;

                if (name.charAt(0) != '.') {

                    int dotIdx = name.indexOf('.', 1);

                    if (dotIdx > 0) {
                        simpleName = name.substring(0, dotIdx);
                    } else {
                        int questionIdx = name.indexOf('?');
                        if (questionIdx > 0) {
                            simpleName = name.substring(0, questionIdx);
                        }
                    }

                } else {

                    Matcher matcher = ATT_PATTERN.matcher(name);
                    if (matcher.matches()) {
                        simpleName = matcher.group(1);
                    } else {
                        simpleName = null;
                    }
                }

                if (StringUtils.isNotBlank(simpleName)) {

                    if (name.endsWith("?assoc") || name.endsWith("{assoc}")) {
                        value = convertAssocValue(value, aliasToRecordRef);
                    }

                    attributes.put(simpleName, value);
                }
            });

            record.setAttributes(attributes);

            RecordsMutation sourceMut = new RecordsMutation();
            sourceMut.setRecords(Collections.singletonList(record));
            RecordsMutResult recordMutResult = recordsResolver.mutate(sourceMut);

            if (i == 0) {
                result.merge(recordMutResult);
            }

            List<RecordMeta> resultRecords = recordMutResult.getRecords();
            for (RecordMeta resultMeta : resultRecords) {
                String alias = record.get(RecordConstants.ATT_ALIAS, "");
                if (StringUtils.isNotBlank(alias)) {
                    aliasToRecordRef.put(alias, resultMeta.getId());
                }
            }
        }

        return result;
    }

    private JsonNode convertAssocValue(JsonNode value, Map<String, RecordRef> mapping) {
        if (value.isTextual()) {
            String textValue = value.asText();
            if (mapping.containsKey(textValue)) {
                return TextNode.valueOf(mapping.get(textValue).toString());
            }
        } else if (value.isArray()) {
            ArrayNode convertedValue = JsonNodeFactory.instance.arrayNode();
            for (JsonNode node : value) {
                convertedValue.add(convertAssocValue(node, mapping));
            }
            return convertedValue;
        }
        return value;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        return recordsResolver.delete(deletion);
    }

    /* OTHER */

    private RecordMeta extractOne(RecordsResult<RecordMeta> values, RecordRef record) {

        if (values.getRecords().isEmpty()) {
            return new RecordMeta(record);
        }
        RecordMeta meta = values.getRecords()
                                .stream()
                                .filter(r -> record.equals(r.getId()))
                                .findFirst()
                                .orElse(null);
        if (meta == null) {
            meta = new RecordMeta(record);
        }
        return meta;
    }

    private Map<String, String> toAttributesMap(Collection<String> attributes) {
        Map<String, String> attributesMap = new HashMap<>();
        for (String attribute : attributes) {
            attributesMap.put(attribute, attribute);
        }
        return attributesMap;
    }

    @Override
    public void register(RecordsDAO recordsSource) {

        String id = recordsSource.getId();
        if (id == null) {
            throw new IllegalArgumentException("id is a mandatory parameter for RecordsDAO");
        }

        if (recordsResolver instanceof RecordsDAORegistry) {
            ((RecordsDAORegistry) recordsResolver).register(recordsSource);
        } else {
            throw new IllegalStateException("Records resolver doesn't support source registration");
        }
    }
}
