package ru.citeck.ecos.records2.evaluator.evaluators;

import lombok.Data;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.model.Predicate;

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
        return predicateService.isMatch(new RecordElement(new RecordMeta(meta)), predicate);
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
