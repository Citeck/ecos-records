package ru.citeck.ecos.records2.type;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Collections;
import java.util.List;

public class DefaultRecordTypeService implements RecordTypeService {

    @NotNull
    public List<ComputedAttribute> getComputedAttributes(RecordRef type) {
        return Collections.emptyList();
    }
}
