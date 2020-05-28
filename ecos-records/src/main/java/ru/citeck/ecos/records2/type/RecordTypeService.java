package ru.citeck.ecos.records2.type;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;

public interface RecordTypeService {

    @NotNull
    List<ComputedAttribute> getComputedAttributes(RecordRef type);
}
