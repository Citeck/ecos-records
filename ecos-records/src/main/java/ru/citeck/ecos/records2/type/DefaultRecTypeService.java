package ru.citeck.ecos.records2.type;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;

import java.util.Collections;
import java.util.List;

public class DefaultRecTypeService implements RecordTypeService {

    public DefaultRecTypeService(RecordsServiceFactory factory) {

    }

    @NotNull
    public List<ComputedAtt> getComputedAtts(@NotNull RecordRef type) {

        /*ComputedAttsDto attsDto = recordsService.getAtts(type, ComputedAttsDto.class);
        if (attsDto.computedAtts == null) {
            return Collections.emptyList();
        }
        return attsDto.getComputedAtts();*/
        return Collections.emptyList();
    }

    @Data
    public static final class ComputedAttsDto {
        private List<ComputedAtt> computedAtts;
    }
}
