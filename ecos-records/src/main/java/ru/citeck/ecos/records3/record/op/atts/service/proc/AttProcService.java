package ru.citeck.ecos.records3.record.op.atts.service.proc;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.AttSchemaReader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AttProcService {

    private final static String PROC_ATT_ALIAS_PREFIX = "__proc_att_";

    private final Map<String, AttProcessor> processors = new ConcurrentHashMap<>();
    private final AttSchemaReader attSchemaReader;

    public AttProcService(RecordsServiceFactory serviceFactory) {
        attSchemaReader = serviceFactory.getAttSchemaReader();
    }

    public DataValue process(ObjectData meta,
                             DataValue value,
                             List<AttProcDef> processorsDef) {


        if (processorsDef.size() == 0) {
            return value;
        }

        DataValue newValue = value;

        for (AttProcDef def : processorsDef) {
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
            value = newValue;
        }

        return value;
    }

    private Set<String> getAttsToLoadSet(List<AttProcDef> processorsDef) {

        if (processorsDef.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> attributes = new HashSet<>();

        for (AttProcDef def : processorsDef) {
            AttProcessor proc = getProcessor(def.getType());
            if (proc != null) {
                attributes.addAll(proc.getAttributesToLoad(def.getArguments()));
            }
        }

        return attributes;
    }

    @NotNull
    public List<SchemaAtt> getProcessorsAtts(List<SchemaAtt> attributes) {

        Set<String> attributesToLoad = new HashSet<>();

        for (SchemaAtt att : attributes) {
            attributesToLoad.addAll(getAttsToLoadSet(att.getProcessors()));
        }

        if (attributesToLoad.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> procAtts = new HashMap<>();

        for (String att : attributesToLoad) {
            procAtts.put(PROC_ATT_ALIAS_PREFIX + att, att);
        }

        return attSchemaReader.read(procAtts);
    }

    public ObjectData applyProcessors(ObjectData data, Map<String, List<AttProcDef>> processors) {

        if (processors.isEmpty()) {
            return data;
        }

        Map<String, Object> dataMap = new HashMap<>();
        data.forEach(dataMap::put);

        return ObjectData.create(applyProcessors(dataMap, processors));
    }

    public Map<String, Object> applyProcessors(Map<String, Object> data, Map<String, List<AttProcDef>> processors) {

        ObjectData procData = ObjectData.create();
        Map<String, Object> resultData = new HashMap<>();

        data.forEach((k, v) -> {
            if (k.startsWith(PROC_ATT_ALIAS_PREFIX)) {
                procData.set(k.replaceFirst(PROC_ATT_ALIAS_PREFIX, ""), v);
            } else {
                resultData.put(k, v);
            }
        });

        if (processors.isEmpty()) {
            return resultData;
        }

        processors.forEach((att, attProcessors) -> {
            if (!attProcessors.isEmpty()) {
                DataValue value = DataValue.create(resultData.get(att));
                resultData.put(att, process(procData, value, attProcessors));
            }
        });

        return resultData;
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
