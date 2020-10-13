package ru.citeck.ecos.records3.record.dao;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"})
public class RecordsDaoInfo {

    @NotNull
    private String id = "";
    @NotNull
    private List<String> supportedLanguages = Collections.emptyList();

    private String columnsSourceId;

    private final Features features = new Features();

    @Data
    public static class Features {
        private boolean query;
        private boolean getAtts;
        private boolean mutate;
        private boolean delete;
    }
}
