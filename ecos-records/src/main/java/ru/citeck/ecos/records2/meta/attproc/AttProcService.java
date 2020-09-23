package ru.citeck.ecos.records2.meta.attproc;

import ecos.com.fasterxml.jackson210.databind.JavaType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AttProcService {

    private final Map<String, AttProcessor> processors = new ConcurrentHashMap<>();

    public DataValue process(ObjectData meta,
                             DataValue value,
                             List<AttProcessorDef> processorsDef,
                             JavaType expectedType) {

        if (processorsDef.size() == 0) {
            return value;
        }

        DataValue newValue = value;

        for (AttProcessorDef def : processorsDef) {
            AttProcessor proc = getProcessor(def.getType());
            if (proc != null) {
                Object newValueObj = proc.process(meta, newValue, def.getArguments());
                if (newValueObj instanceof String) {
                    newValue = DataValue.createStr(newValueObj);
                } else {
                    newValue = DataValue.create(newValueObj);
                }
            }
        }

        if (!Objects.equals(value, newValue)) {
            value = Json.getMapper().convert(newValue, expectedType);
        }

        return value;
    }

    public Set<String> getAttributesToLoad(List<AttProcessorDef> processorsDef) {

        if (processorsDef.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> attributes = new HashSet<>();

        for (AttProcessorDef def : processorsDef) {
            AttProcessor proc = getProcessor(def.getType());
            if (proc != null) {
                attributes.addAll(proc.getAttributesToLoad(def.getArguments()).values());
            }
        }

        return attributes;
    }

    @Nullable
    private AttProcessor getProcessor(String type) {
        AttProcessor proc = processors.get(type);
        if (proc == null) {
            log.error("Attribute processor doesn't found for type '" + type + "'");
        }
        return proc;
    }

    public void register(AttProcessor processor) {
        this.processors.put(processor.getType(), processor);
    }
}
