package ru.citeck.ecos.records2.type;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Map;

public interface RecordTypeService {

    @NotNull
    Map<String, ComputedAttribute> getComputedAttributes(RecordRef type);
}
