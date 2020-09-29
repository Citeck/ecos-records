package ru.citeck.ecos.records3.record.operation.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.*;

public class AttOrElseProcessor extends AbstractAttProcessor<List<DataValue>> {

    public AttOrElseProcessor() {
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
                if (txtAtt.length() > 0) {
                    if (Character.isDigit(txtAtt.charAt(0))) {
                        value = DataValue.create(orElseAtt.asDouble());
                    } else if (isBool(txtAtt)) {
                        value = DataValue.create(orElseAtt.asBoolean());
                    } else {
                        value = DataValue.create(txtAtt.substring(1, txtAtt.length() - 1));
                    }
                } else {
                    value = orElseAtt;
                }
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

    private boolean isBool(String value) {
        return Boolean.TRUE.toString().equals(value)
            || Boolean.FALSE.toString().equals(value);
    }

    private boolean isAttToLoadValue(String value) {
        if (isBool(value) || Character.isDigit(value.charAt(0))) {
            return false;
        }
        return value.charAt(0) != '\'' && value.charAt(0) != '"';
    }
}
