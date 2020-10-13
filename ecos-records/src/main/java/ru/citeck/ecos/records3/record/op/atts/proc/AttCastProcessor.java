package ru.citeck.ecos.records3.record.op.atts.proc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.List;

public class AttCastProcessor extends AbstractAttProcessor<List<DataValue>> {

    @Override
    protected Object processOne(@NotNull ObjectData meta,
                                @NotNull DataValue value,
                                @NotNull List<DataValue> arguments) {

        if (arguments.size() == 0) {
            return value;
        }

        switch (arguments.get(0).asText()) {
            case "str":
                return value.asText();
            case "num":
                return value.asDouble();
            case "bool":
                return value.asBoolean();
        }

        return null;
    }

    @Override
    protected List<DataValue> parseArgs(@NotNull List<DataValue> arguments) {
        return arguments;
    }

    @NotNull
    @Override
    public String getType() {
        return "cast";
    }
}
