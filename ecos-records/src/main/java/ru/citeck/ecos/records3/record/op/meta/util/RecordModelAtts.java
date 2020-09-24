package ru.citeck.ecos.records3.record.op.meta.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RecordModelAtts {
    private final Map<String, String> recordAtts;
    private final Map<String, String> modelAtts;
}
