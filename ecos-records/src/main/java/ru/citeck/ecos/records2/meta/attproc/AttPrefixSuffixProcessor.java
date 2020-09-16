package ru.citeck.ecos.records2.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordMeta;

import java.util.List;

public class AttPrefixSuffixProcessor implements AttProcessor {

    @NotNull
    @Override
    public Object process(@NotNull RecordMeta meta, @NotNull DataValue value, @NotNull List<DataValue> arguments) {

        String prefix = "";
        String suffix = "";

        if (arguments.size() > 0) {
            prefix = arguments.get(0).asText();
        }
        if (arguments.size() > 1) {
            suffix = arguments.get(1).asText();
        }

        return prefix + value.asText() + suffix;
    }

    @Override
    public String getType() {
        return "presuf";
    }
}
