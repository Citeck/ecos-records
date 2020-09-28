package ru.citeck.ecos.records3.type;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;

import java.util.Collections;
import java.util.List;

public class DefaultRecTypeService implements RecTypeService {

    private final RecordsService recordsService;

    public DefaultRecTypeService(RecordsServiceFactory factory) {
        recordsService = factory.getRecordsService();
    }

    @NotNull
    public List<ComputedAtt> getComputedAtts(RecordRef type) {

        ComputedAttsMeta meta = recordsService.getAtts(type, ComputedAttsMeta.class);
        if (meta == null || meta.computedAtts == null) {
            return Collections.emptyList();
        }
        return meta.getComputedAtts();
    }

    @Data
    public static final class ComputedAttsMeta {
        private List<ComputedAtt> computedAtts;
    }
}
