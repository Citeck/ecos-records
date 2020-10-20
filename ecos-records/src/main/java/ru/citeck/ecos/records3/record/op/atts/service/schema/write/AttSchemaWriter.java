package ru.citeck.ecos.records3.record.op.atts.service.schema.write;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.NullNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.NameUtils;
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttProcessorDef;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaRootAtt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface AttSchemaWriter {

    default Map<String, String> writeToMap(List<SchemaRootAtt> schema) {

        Map<String, String> result = new HashMap<>();
        schema.forEach(att ->
            result.put(att.getAttribute().getAlias(), write(att))
        );
        return result;
    }

    default void writeInnerAtts(List<SchemaRootAtt> schema, StringBuilder sb) {
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

    default String write(SchemaRootAtt att) {

        List<AttProcessorDef> processors = att.getProcessors();

        StringBuilder sb = new StringBuilder();
        write(att.getAttribute(), sb);

        for (AttProcessorDef processor : processors) {
            sb.append("|").append(processor.getType()).append("(");
            int argsSize = processor.getArguments().size();
            for (int i = 0; i < argsSize; i++) {
                sb.append(processor.getArguments().get(i).toString());
                if (i < argsSize - 1) {
                    sb.append(',');
                }
            }
            sb.append(")");
        }

        return sb.toString();
    }

    default String write(SchemaAtt attribute) {
        StringBuilder sb = new StringBuilder();
        write(attribute, sb);
        return sb.toString();
    }

    void write(SchemaAtt attribute, StringBuilder out);
}
