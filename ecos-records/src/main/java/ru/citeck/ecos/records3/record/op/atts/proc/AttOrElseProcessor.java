package ru.citeck.ecos.records3.record.op.atts.proc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.*;

public class AttOrElseProcessor extends AbstractAttProcessor<List<DataValue>> {

    public static final String ATT_PREFIX = "a:";

    public AttOrElseProcessor() {
        super(true);
    }

    @Override
    protected Object processOne(@NotNull ObjectData meta,
                                @NotNull DataValue value,
                                @NotNull List<DataValue> arguments) {

        if (arguments.isEmpty() || value.isNotNull()) {
            return value;
        }

        for (DataValue orElseAtt : arguments) {

            if (orElseAtt.isTextual()) {
                String txtAtt = orElseAtt.asText();
                if (txtAtt.startsWith(ATT_PREFIX)) {
                    value = meta.get(txtAtt.substring(ATT_PREFIX.length()));
                } else {
                    value = orElseAtt;
                }
            } else {
                value = orElseAtt;
            }
            if (value.isNotNull() && (!value.isTextual() || !value.asText().isEmpty())) {
                return value;
            }
        }

        return value;
    }

    @Override
    protected List<DataValue> parseArgs(@NotNull List<DataValue> arguments) {
        return arguments;
    }

    @NotNull
    @Override
    public String getType() {
        return "or";
    }

    @NotNull
    @Override
    public Collection<String> getAttributesToLoad(@NotNull List<DataValue> arguments) {

        Set<String> attsToLoad = new HashSet<>();

        for (DataValue orElseAtt : arguments) {
            if (!orElseAtt.isTextual()) {
                continue;
            }
            String txtAtt = orElseAtt.asText();
            if (txtAtt.startsWith(ATT_PREFIX) && txtAtt.length() > ATT_PREFIX.length()) {
                attsToLoad.add(txtAtt.substring(ATT_PREFIX.length()));
            }
        }

        return attsToLoad;
    }
}
