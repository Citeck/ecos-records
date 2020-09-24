package ru.citeck.ecos.records3.source.info;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"})
public class RecsSourceInfo {

    @NotNull
    private String id = "";
    @NotNull
    private List<String> supportedLanguages = Collections.emptyList();

    private String columnsSourceId;

    private final Map<RecsDaoFeature, Boolean> features = new HashMap<>();

    public void setFeature(RecsDaoFeature feature, boolean enabled) {
        features.put(feature, enabled);
    }
}
