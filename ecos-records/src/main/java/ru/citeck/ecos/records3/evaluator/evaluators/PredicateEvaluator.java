package ru.citeck.ecos.records3.evaluator.evaluators;

import lombok.Data;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.ServiceFactoryAware;
import ru.citeck.ecos.records3.evaluator.RecordEvaluator;
import ru.citeck.ecos.records3.predicate.PredicateService;
import ru.citeck.ecos.records3.predicate.PredicateUtils;
import ru.citeck.ecos.records3.predicate.RecordElement;
import ru.citeck.ecos.records3.predicate.model.Predicate;

import java.util.List;

public class PredicateEvaluator implements RecordEvaluator<List<String>, RecordAtts, PredicateEvaluator.Config>,
                                           ServiceFactoryAware {

    public static final String TYPE = "predicate";

    private PredicateService predicateService;

    @Override
    public List<String> getMetaToRequest(Config config) {
        Predicate predicate = Json.getMapper().convert(config.predicate, Predicate.class);
        return PredicateUtils.getAllPredicateAttributes(predicate);
    }

    @Override
    public boolean evaluate(RecordAtts meta, Config config) {
        Predicate predicate = Json.getMapper().convert(config.predicate, Predicate.class);
        return predicateService.isMatch(new RecordElement(meta), predicate);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {
        this.predicateService = serviceFactory.getPredicateService();
    }

    @Data
    public static class Config {
        private Predicate predicate;
    }
}
