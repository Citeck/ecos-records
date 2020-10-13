package ru.citeck.ecos.records3.record.op.atts.schema.read;

import lombok.Data;
import ru.citeck.ecos.records3.record.op.atts.proc.AttProcessorDef;

import java.util.List;

@Data
public class AttWithProc {
    private final String attribute;
    private final List<AttProcessorDef> processors;
}
