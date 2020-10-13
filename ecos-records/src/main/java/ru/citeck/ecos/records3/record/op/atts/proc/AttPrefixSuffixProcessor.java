package ru.citeck.ecos.records3.record.op.atts.proc;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.List;

public class AttPrefixSuffixProcessor extends AbstractAttProcessor<AttPrefixSuffixProcessor.Args> {

    @Override
    protected Object processOne(@NotNull ObjectData meta, @NotNull DataValue value, @NotNull Args args) {
        return args.prefix + value.asText() + args.suffix;
    }

    @Override
    protected Args parseArgs(@NotNull List<DataValue> arguments) {

        String prefix = "";
        String suffix = "";

        if (arguments.size() > 0) {
            prefix = arguments.get(0).asText();
        }
        if (arguments.size() > 1) {
            suffix = arguments.get(1).asText();
        }
        return new Args(prefix, suffix);
    }

    @NotNull
    @Override
    public String getType() {
        return "presuf";
    }

    @Data
    @RequiredArgsConstructor
    public static class Args {
        private final String prefix;
        private final String suffix;
    }
}
