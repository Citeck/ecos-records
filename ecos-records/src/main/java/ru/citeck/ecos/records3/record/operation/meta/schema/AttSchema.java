package ru.citeck.ecos.records3.record.operation.meta.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
public class AttSchema {

    @NotNull
    private final List<SchemaRootAtt> attributes;

    public AttSchema() {
        attributes = Collections.emptyList();
    }
}
