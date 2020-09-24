package ru.citeck.ecos.records3.type;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;

import java.util.List;

public interface RecordTypeService {

    @NotNull
    List<ComputedAttribute> getComputedAttributes(RecordRef type);
}
