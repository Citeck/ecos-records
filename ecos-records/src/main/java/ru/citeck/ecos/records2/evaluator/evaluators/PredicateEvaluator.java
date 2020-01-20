package ru.citeck.ecos.records2.evaluator.evaluators;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.PredicateUtils;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.predicate.RecordElement;

import java.util.List;

public class PredicateEvaluator implements RecordEvaluator<List<String>, RecordMeta, PredicateEvaluator.Config>,
                                           ServiceFactoryAware {

    public static final String TYPE = "predicate";

    private PredicateService predicateService;

    @Override
    public List<String> getMetaToRequest(Config config) {
        Predicate predicate = predicateService.readJson(config.predicate);
        return PredicateUtils.getAllPredicateAttributes(predicate);
    }

    @Override
    public boolean evaluate(RecordMeta meta, Config config) {
        Predicate predicate = predicateService.readJson(config.predicate);
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
        private ObjectNode predicate;
    }
}
