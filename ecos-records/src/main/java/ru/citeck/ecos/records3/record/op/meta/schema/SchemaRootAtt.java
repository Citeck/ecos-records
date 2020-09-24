package ru.citeck.ecos.records3.record.op.meta.schema;

import ecos.com.fasterxml.jackson210.databind.JavaType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.citeck.ecos.records3.record.op.meta.attproc.AttProcessorDef;

import java.util.List;

@Data
@RequiredArgsConstructor
public class SchemaRootAtt {

    private final SchemaAtt attribute;
    private final List<AttProcessorDef> processors;
    private final JavaType type;
}
