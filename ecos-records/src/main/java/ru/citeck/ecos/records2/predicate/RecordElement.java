package ru.citeck.ecos.records2.predicate;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.predicate.element.Element;
import ru.citeck.ecos.records2.predicate.element.elematts.ElementAttributes;
import ru.citeck.ecos.records3.record.atts.schema.ScalarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use RecordAttsElement instead
 */
@Deprecated
public class RecordElement implements Element {

    private final RecordRef recordRef;
    private RecordsService recordsService;
    private RecordMeta meta;

    public RecordElement(RecordMeta meta) {
        this.meta = meta;
        this.recordRef = meta.getId();
    }

    public RecordElement(RecordsService recordsService, RecordRef recordRef) {
        this.recordRef = recordRef;
        this.recordsService = recordsService;
    }

    public RecordRef getRecordRef() {
        return recordRef;
    }

    public RecordsService getRecordsService() {
        return recordsService;
    }

    public RecordMeta getMeta() {
        return meta;
    }

    public void addAttributes(RecordMeta meta) {
        if (this.meta == null) {
            this.meta = new RecordMeta(meta);
            return;
        }
        meta.forEachJ((k, v) -> this.meta.set(k, v));
    }

    @Override
    public ElementAttributes getAttributes(List<String> attributes) {
        if (recordsService != null && recordRef != null) {
            if (meta == null) {
                meta = recordsService.getAttributes(recordRef, getQueryAtts(attributes));
            } else {
                List<String> missingAttributes = new ArrayList<>();
                for (String att : attributes) {
                    if (!meta.has(att)) {
                        missingAttributes.add(att);
                    }
                }
                RecordMeta atts = recordsService.getAttributes(recordRef, getQueryAtts(missingAttributes));
                atts.forEachJ((k, v) -> meta.set(k, v));
            }
        }
        return new Attributes(meta);
    }

    static Map<String, String> getQueryAtts(List<String> attributes) {

        Map<String, String> attributesMap = new HashMap<>();
        for (String att : attributes) {
            if (att.indexOf('?') == -1) {
                attributesMap.put(att, att + ScalarType.RAW.getSchema());
            } else {
                attributesMap.put(att, att);
            }
        }

        return attributesMap;
    }

    private static class Attributes implements ElementAttributes {

        RecordMeta meta;

        Attributes(RecordMeta meta) {
            this.meta = meta;
        }

        @Override
        public Object getAttribute(String name) {
            return meta != null ? meta.get(name) : null;
        }
    }
}
