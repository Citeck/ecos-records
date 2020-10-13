package ru.citeck.ecos.records3.record.op.atts.schema.write;

import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import ru.citeck.ecos.records3.record.op.atts.proc.AttProcessorDef;
import ru.citeck.ecos.records3.record.op.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.schema.SchemaRootAtt;

import java.util.HashMap;
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

    default String write(SchemaAtt attribute) {
        StringBuilder sb = new StringBuilder();
        write(attribute, sb);
        return sb.toString();
    }

    void write(SchemaAtt attribute, StringBuilder out);
}
