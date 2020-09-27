package ru.citeck.ecos.records3.predicate;

import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordElement implements Element {

    private final RecordRef recordRef;
    private RecordsService recordsService;
    private RecordAtts meta;

    public RecordElement(RecordAtts meta) {
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

    public RecordAtts getMeta() {
        return meta;
    }

    public void addAttributes(RecordAtts meta) {
        if (this.meta == null) {
            this.meta = new RecordAtts(meta);
            return;
        }
        meta.forEach((k, v) -> this.meta.set(k, v));
    }

    @Override
    public ElementAttributes getAttributes(List<String> attributes) {
        if (recordsService != null && recordRef != null) {
            if (meta == null) {
                meta = recordsService.getAtts(recordRef, getQueryAtts(attributes));
            } else {
                List<String> missingAttributes = new ArrayList<>();
                for (String att : attributes) {
                    if (!meta.has(att)) {
                        missingAttributes.add(att);
                    }
                }
                RecordAtts atts = recordsService.getAtts(recordRef, getQueryAtts(missingAttributes));
                atts.forEach((k, v) -> meta.set(k, v));
            }
        }
        return new Attributes(meta);
    }

    static Map<String, String> getQueryAtts(List<String> attributes) {

        Map<String, String> attributesMap = new HashMap<>();
        for (String att : attributes) {
            if (att.indexOf('?') == -1) {
                attributesMap.put(att, att + "?str");
            } else {
                attributesMap.put(att, att);
            }
        }

        return attributesMap;
    }

    private static class Attributes implements ElementAttributes {

        RecordAtts meta;

        Attributes(RecordAtts meta) {
            this.meta = meta;
        }

        @Override
        public Object getAttribute(String name) {
            return meta != null ? meta.get(name) : null;
        }
    }
}
