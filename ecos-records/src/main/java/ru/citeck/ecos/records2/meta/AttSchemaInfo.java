package ru.citeck.ecos.records2.meta;

import lombok.Data;
import ru.citeck.ecos.records2.meta.attproc.AttProcessorDef;

import java.util.Collections;
import java.util.List;

@Data
public class AttSchemaInfo {
    private String originalKey;
    private List<AttProcessorDef> processors = Collections.emptyList();
}
