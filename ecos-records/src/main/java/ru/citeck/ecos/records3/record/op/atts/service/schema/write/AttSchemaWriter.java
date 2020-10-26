package ru.citeck.ecos.records3.record.op.atts.service.schema.write;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.NullNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.NameUtils;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface AttSchemaWriter {

    default Map<String, String> writeToMap(List<SchemaAtt> schema) {

        Map<String, String> result = new HashMap<>();
        schema.forEach(att ->
            result.put(att.getAlias(), write(att))
        );
        return result;
    }

    default void writeInnerAtts(List<SchemaAtt> schema, StringBuilder sb) {
        writeInnerAtts(writeToMap(schema), sb);
    }

    default void writeInnerAtts(Map<String, String> attributes, StringBuilder sb) {
        attributes.forEach((k, v) -> sb.append(k).append(":").append(v));
    }

    default JsonNode unescapeKeys(JsonNode node) {
        if (node == null) {
            return NullNode.getInstance();
        }
        if (node.size() == 0) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode result = Json.getMapper().newObjectNode();
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                result.set(NameUtils.unescape(name), unescapeKeys(node.path(name)));
            }
            return result;
        }
        if (node.isArray()) {
            ArrayNode array = Json.getMapper().newArrayNode();
            for (JsonNode innerNode : node) {
                array.add(unescapeKeys(innerNode));
            }
            return array;
        }
        return node;
    }

    default String write(SchemaAtt att) {
        StringBuilder sb = new StringBuilder();
        write(att, sb);
        return sb.toString();
    }

    void write(SchemaAtt attribute, StringBuilder out);
}
