package ru.citeck.ecos.records2.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttOrProcessor implements AttProcessor {

    private static final String ATTS_PREFIX = "__or_proc_att";

    @NotNull
    @Override
    public Object process(@NotNull ObjectData meta, @NotNull DataValue value, @NotNull List<DataValue> arguments) {

        if (arguments.isEmpty() || value.isNotNull()) {
            return value;
        }
        for (DataValue orElseAtt : arguments) {
            String txtAtt = orElseAtt.asText();
            if (isMetaAttValue(txtAtt)) {
                value = meta.get(ATTS_PREFIX + txtAtt);
            } else {
                value = DataValue.createStr(txtAtt.substring(1, txtAtt.length() - 1));
            }
            if (value.isNotNull() && (!value.isTextual() || !value.asText().isEmpty())) {
                return value;
            }
        }
        return value;
    }

    @NotNull
    @Override
    public String getType() {
        return "or";
    }

    @NotNull
    @Override
    public Map<String, String> getAttributesToLoad(@NotNull List<DataValue> arguments) {

        Map<String, String> attsToLoad = new HashMap<>();

        for (DataValue orElseAtt : arguments) {
            if (orElseAtt.isNull() || orElseAtt.asText().isEmpty()) {
                continue;
            }
            String att = orElseAtt.asText();
            if (isMetaAttValue(att)) {
                attsToLoad.put(ATTS_PREFIX + att, att);
            }
        }

        return attsToLoad;
    }

    private boolean isMetaAttValue(String value) {
        return value.charAt(0) != '\'' && value.charAt(0) != '"';
    }
}
