package ru.citeck.ecos.records2.meta.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RecordModelAtts {
    private final Map<String, String> recordAtts;
    private final Map<String, String> modelAtts;
}
