package ru.citeck.ecos.records3.evaluator.evaluators;

import lombok.Data;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.evaluator.RecordEvaluator;

import java.util.Collections;
import java.util.Map;

public class HasPermissionEvaluator implements RecordEvaluator<Map<String, String>,
                                                               HasPermissionEvaluator.Meta,
                                                               HasPermissionEvaluator.Config> {

    private static final String PERMISSION_ATT_PATTERN = ".att(n:\"permissions\"){has(n:\"%s\")}";
    private static final String HAS_PERM_PROP = "hasPermission";

    @Override
    public boolean evaluate(Meta meta, Config config) {
        return meta.hasPermission == null || Boolean.TRUE.equals(meta.hasPermission);
    }

    @Override
    public Map<String, String> getMetaToRequest(Config config) {

        if (StringUtils.isBlank(config.permission)) {
            throw new IllegalArgumentException("You need to specify a permission for evaluating. Config:" + config);
        }

        return Collections.singletonMap(HAS_PERM_PROP, String.format(PERMISSION_ATT_PATTERN, config.permission));
    }

    @Override
    public String getType() {
        return "has-permission";
    }

    @Data
    public static class Config {
        private String permission;
    }

    @Data
    public static class Meta {
        private Boolean hasPermission;
    }
}
