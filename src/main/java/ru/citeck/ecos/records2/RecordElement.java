package ru.citeck.ecos.records2;

import ru.citeck.ecos.predicate.Element;
import ru.citeck.ecos.predicate.ElementAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordElement implements Element {

    private RecordRef recordRef;
    private RecordsService recordsService;

    public RecordElement(RecordsService recordsService, RecordRef recordRef) {
        this.recordRef = recordRef;
        this.recordsService = recordsService;
    }

    @Override
    public ElementAttributes getAttributes(List<String> attributes) {

        Map<String, String> attributesMap = new HashMap<>();
        for (String att : attributes) {
            if (att.indexOf('?') == -1) {
                attributesMap.put(att, att + "?str");
            } else {
                attributesMap.put(att, att);
            }
        }

        RecordMeta meta = recordsService.getAttributes(recordRef, attributesMap);
        return new Attributes(meta);
    }

    private static class Attributes implements ElementAttributes {

        RecordMeta meta;

        Attributes(RecordMeta meta) {
            this.meta = meta;
        }

        @Override
        public Object getAttribute(String name) {
            return meta.get(name);
        }
    }
}
