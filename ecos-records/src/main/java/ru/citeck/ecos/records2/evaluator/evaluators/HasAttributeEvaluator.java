package ru.citeck.ecos.records2.evaluator.evaluators;

import lombok.Data;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.Collections;
import java.util.Map;

public class HasAttributeEvaluator implements RecordEvaluator<Map<String, Object>,
                                                              HasAttributeEvaluator.Meta,
                                                              HasAttributeEvaluator.Config> {

    private static final String HAS_ATTRIBUTE_PATTERN = ".has(n:\"%s\")";
    private static final String HAS_ATT_META_FIELD = "hasAtt";

    @Override
    public boolean evaluate(Meta meta, Config config) {
        return Boolean.TRUE.equals(meta.getHasAtt());
    }

    @Override
    public String getType() {
        return "has-attribute";
    }

    @Override
    public Map<String, Object> getMetaToRequest(Config config) {

        if (StringUtils.isBlank(config.attribute)) {
            throw new IllegalArgumentException("You need to specify a attribute, for evaluating. Config:"
                + config.toString());
        }

        return Collections.singletonMap(HAS_ATT_META_FIELD, String.format(HAS_ATTRIBUTE_PATTERN, config.attribute));
    }

    @Data
    public static class Config {
        private String attribute;
    }

    @Data
    public static class Meta {
        private Boolean hasAtt;
    }
}
