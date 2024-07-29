package ru.citeck.ecos.records2.source.info;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@Data
public class RecordsSourceInfo {

    @NotNull
    private String id = "";
    @NotNull
    private List<String> supportedLanguages = Collections.emptyList();

    private String columnsSourceId;

    private boolean queryWithMetaSupported;
    private boolean querySupported;
    private boolean metaSupported;
    private boolean mutationSupported;
}
