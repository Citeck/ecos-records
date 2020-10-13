package ru.citeck.ecos.records2.type;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.RecordsService;

import java.util.Collections;
import java.util.List;

public class DefaultRecTypeService implements RecordTypeService {

    private final RecordsService recordsService;

    public DefaultRecTypeService(RecordsServiceFactory factory) {
        recordsService = factory.getRecordsServiceV1();
    }

    @NotNull
    public List<ComputedAtt> getComputedAtts(@NotNull RecordRef type) {

        ComputedAttsDto attsDto = recordsService.getAtts(type, ComputedAttsDto.class);
        if (attsDto.computedAtts == null) {
            return Collections.emptyList();
        }
        return attsDto.getComputedAtts();
    }

    @Data
    public static final class ComputedAttsDto {
        private List<ComputedAtt> computedAtts;
    }
}
