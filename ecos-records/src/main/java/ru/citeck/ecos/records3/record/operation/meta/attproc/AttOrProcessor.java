package ru.citeck.ecos.records3.record.operation.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.*;

public class AttOrProcessor extends AbstractAttProcessor<List<DataValue>> {

    public AttOrProcessor() {
        super(true);
    }

    @Override
    protected Object processOne(@NotNull ObjectData meta, @NotNull DataValue value, @NotNull List<DataValue> arguments) {

        if (arguments.isEmpty() || value.isNotNull()) {
            return value;
        }

        for (DataValue orElseAtt : arguments) {
            String txtAtt = orElseAtt.asText();
            if (isAttToLoadValue(txtAtt)) {
                value = meta.get(txtAtt);
            } else {
                value = DataValue.createStr(txtAtt.substring(1, txtAtt.length() - 1));
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
            if (orElseAtt.isNull() || orElseAtt.asText().isEmpty()) {
                continue;
            }
            String att = orElseAtt.asText();
            if (isAttToLoadValue(att)) {
                attsToLoad.add(att);
            }
        }

        return attsToLoad;
    }

    private boolean isAttToLoadValue(String value) {
        return value.charAt(0) != '\'' && value.charAt(0) != '"';
    }
}
