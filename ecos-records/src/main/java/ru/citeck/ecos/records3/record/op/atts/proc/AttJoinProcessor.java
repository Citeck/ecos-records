package ru.citeck.ecos.records3.record.op.atts.proc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.List;

public class AttJoinProcessor implements AttProcessor {

    @NotNull
    @Override
    public Object process(@NotNull ObjectData meta,
                          @NotNull DataValue value,
                          @NotNull List<DataValue> arguments) {

        if (!value.isArray()) {
            return value;
        }
        if (value.size() == 0) {
            return "";
        }

        String delim = arguments.size() > 0 ? arguments.get(0).asText() : ",";

        StringBuilder sb = new StringBuilder();
        value.forEach(v -> sb.append(v.asText()).append(delim));
        return DataValue.createStr(sb.substring(0, sb.length() - delim.length()));
    }

    @NotNull
    @Override
    public String getType() {
        return "join";
    }
}
