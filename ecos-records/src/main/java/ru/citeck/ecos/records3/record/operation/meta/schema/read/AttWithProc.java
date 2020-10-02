package ru.citeck.ecos.records3.record.operation.meta.schema.read;

import lombok.Data;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcessorDef;

import java.util.List;

@Data
public class AttWithProc {
    private final String attribute;
    private final List<AttProcessorDef> processors;
}
