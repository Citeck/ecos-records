package ru.citeck.ecos.records2.meta;

import ru.citeck.ecos.commons.utils.TmplUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;

import java.util.Set;

public class RecordsTemplateService {

    private final RecordsService recordsService;

    public RecordsTemplateService(RecordsServiceFactory recordsServiceFactory) {
        recordsService = recordsServiceFactory.getRecordsService();
    }

    public <T> T resolve(T template, RecordRef recordRef) {

        if (template == null || recordRef == null) {
            return template;
        }

        Set<String> atts = TmplUtils.getAtts(template);
        if (atts.isEmpty()) {
            return template;
        }

        RecordMeta meta = recordsService.getAttributes(recordRef, atts);

        return TmplUtils.applyAtts(template, meta.getAttributes());
    }
}
