package ru.citeck.ecos.records3.record.op.atts.service.proc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface AttProcessor {

    @NotNull
    Object process(@NotNull ObjectData attributes, @NotNull DataValue value, @NotNull List<DataValue> arguments);

    @NotNull
    String getType();

    @NotNull
    default Collection<String> getAttributesToLoad(@NotNull List<DataValue> arguments) {
        return Collections.emptySet();
    }
}
