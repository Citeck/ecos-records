package ru.citeck.ecos.records3.record.operation.meta.schema;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcessorDef;

import java.util.List;

@Data
@RequiredArgsConstructor
public class SchemaRootAtt {

    private final SchemaAtt attribute;
    private final List<AttProcessorDef> processors;
}
