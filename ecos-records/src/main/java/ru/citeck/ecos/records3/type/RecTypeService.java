package ru.citeck.ecos.records3.type;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;

import java.util.List;

public interface RecTypeService {

    @NotNull
    List<ComputedAtt> getComputedAtts(RecordRef type);
}
