package ru.citeck.ecos.records2.meta;

import ru.citeck.ecos.commons.utils.TmplUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsServiceFactory;

import java.util.ArrayList;
import java.util.Set;

public class RecordsTemplateService {

    private RecordsMetaService recordsMetaService;

    public RecordsTemplateService(RecordsServiceFactory recordsServiceFactory) {
        recordsMetaService = recordsServiceFactory.getRecordsMetaService();
    }

    public <T> T resolve(T template, Object record) {

        if (template == null || record == null) {
            return template;
        }

        Set<String> atts = TmplUtils.getAtts(template);
        if (atts.isEmpty()) {
            return template;
        }

        RecordMeta meta = recordsMetaService.getMeta(record, new ArrayList<>(atts));

        return TmplUtils.applyAtts(template, meta.getAttributes());
    }
}
