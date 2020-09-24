package ru.citeck.ecos.records3.record.operation.meta.schema.write;

import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records3.record.operation.meta.schema.AttsSchema;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AttSchemaWriter {

    default Map<String, String> writeAsMap(AttsSchema schema) {

        Map<String, String> result = new HashMap<>();
        schema.getAttributes().forEach(att ->
            result.put(att.getAttribute().getAlias(), write(att))
        );
        return result;
    }

    default void writeInnerAtts(AttsSchema schema, StringBuilder sb) {
        writeInnerAtts(writeAsMap(schema), sb);
    }

    default void writeInnerAtts(Map<String, String> attributes, StringBuilder sb) {
        attributes.forEach((k, v) -> sb.append(k).append(":").append(v));
    }

    default String write(SchemaRootAtt att) {

        List<AttProcessorDef> processors = att.getProcessors();

        StringBuilder sb = new StringBuilder();
        write(att.getAttribute(), sb);

        for (AttProcessorDef processor : processors) {
            sb.append("|").append(processor.getType()).append("(");
            int argsSize = processor.getArguments().size();
            for (int i = 0; i < argsSize; i++) {
                sb.append(TextNode.valueOf(processor.getArguments().get(i).toString()).toString());
                if (i < argsSize - 1) {
                    sb.append(',');
                }
            }
        }

        return sb.toString();
    }

    void write(SchemaAtt attribute, StringBuilder out);
}
