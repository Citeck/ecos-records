package ru.citeck.ecos.records2.type;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;

import java.util.Collections;
import java.util.List;

public class DefaultRecordTypeService implements RecordTypeService {

    private final RecordsService recordsService;

    public DefaultRecordTypeService(RecordsServiceFactory factory) {
        recordsService = factory.getRecordsService();
    }

    @NotNull
    public List<ComputedAttribute> getComputedAttributes(RecordRef type) {

        ComputedAttsMeta meta = recordsService.getMeta(type, ComputedAttsMeta.class);
        if (meta == null || meta.computedAttributes == null) {
            return Collections.emptyList();
        }
        return meta.getComputedAttributes();
    }

    @Data
    public static final class ComputedAttsMeta {
        private List<ComputedAttribute> computedAttributes;
    }
}
