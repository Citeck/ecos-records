package ru.citeck.ecos.records2.evaluator;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.evaluator.details.EvalDetails;
import ru.citeck.ecos.records2.evaluator.details.EvalDetailsImpl;
import ru.citeck.ecos.records2.evaluator.evaluators.*;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class RecordEvaluatorServiceImpl implements RecordEvaluatorService, ServiceFactoryAware {

    private RecordsService recordsService;
    private DtoSchemaReader dtoSchemaReader;
    private AttSchemaWriter attSchemaWriter;
    private RecordsServiceFactory factory;

    private final Map<String, ParameterizedRecordEvaluator> evaluators = new ConcurrentHashMap<>();

    @Override
    public boolean evaluate(EntityRef recordRef, RecordEvaluatorDto evaluator) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);
        List<EntityRef> recordRefs = Collections.singletonList(recordRef);

        Map<EntityRef, List<Boolean>> evaluateResult = evaluate(recordRefs, evaluators);
        return evaluateResult.get(recordRef).get(0);
    }

    @Override
    public Map<EntityRef, Boolean> evaluate(List<EntityRef> recordRefs, RecordEvaluatorDto evaluator) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);

        Map<EntityRef, List<Boolean>> evaluateResult = evaluate(recordRefs, evaluators);
        Map<EntityRef, Boolean> result = new HashMap<>();

        evaluateResult.forEach((ref, b) -> result.put(ref, b.get(0)));

        return result;
    }

    @Override
    public Map<EntityRef, List<Boolean>> evaluate(List<EntityRef> recordRefs,
                                                  List<RecordEvaluatorDto> evaluators) {

        Map<EntityRef, List<EvalDetails>> details = evalWithDetails(recordRefs, evaluators);
        Map<EntityRef, List<Boolean>> result = new HashMap<>();

        details.forEach((k, v) -> {
            List<Boolean> resultsList = result.computeIfAbsent(k, kk -> new ArrayList<>());
            v.forEach(d -> resultsList.add(d.getResult()));
        });

        return result;
    }

    @Override
    public EvalDetails evalWithDetails(EntityRef recordRef,
                                       RecordEvaluatorDto evaluator) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);
        List<EntityRef> recordRefs = Collections.singletonList(recordRef);

        Map<EntityRef, List<EvalDetails>> evaluateResult = evalWithDetails(recordRefs, evaluators);
        return evaluateResult.get(recordRef).get(0);
    }

    @Override
    public Map<EntityRef, EvalDetails> evalWithDetails(List<EntityRef> recordRefs,
                                                       RecordEvaluatorDto evaluator) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);

        Map<EntityRef, List<EvalDetails>> evaluateResult = evalWithDetails(recordRefs, evaluators);
        Map<EntityRef, EvalDetails> result = new HashMap<>();

        evaluateResult.forEach((ref, b) -> result.put(ref, b.get(0)));

        return result;
    }

    @Override
    public Map<EntityRef, List<EvalDetails>> evalWithDetails(List<EntityRef> recordRefs,
                                                             List<RecordEvaluatorDto> evaluators) {

        List<Map<String, String>> metaAttributes = getRequiredMetaAttributes(evaluators);
        Set<String> attsToRequest = new HashSet<>();

        metaAttributes.forEach(atts -> attsToRequest.addAll(atts.values()));

        List<RecordAtts> recordsMeta;
        if (!attsToRequest.isEmpty()) {
            recordsMeta = recordsService.getAtts(recordRefs, attsToRequest);
        } else {
            recordsMeta = recordRefs.stream().map(RecordAtts::new).collect(Collectors.toList());
        }

        Map<EntityRef, List<EvalDetails>> evalResultsByRecord = new HashMap<>();

        for (int i = 0; i < recordRefs.size(); i++) {
            RecordAtts meta = recordsMeta.get(i);
            List<EvalDetails> evalResult = evaluateWithMeta(evaluators, meta);
            evalResultsByRecord.put(recordRefs.get(i), evalResult);
        }

        return evalResultsByRecord;
    }

    private List<EvalDetails> evaluateWithMeta(List<RecordEvaluatorDto> evaluators,
                                               RecordAtts record) {

        List<EvalDetails> result = new ArrayList<>();
        for (RecordEvaluatorDto evaluator : evaluators) {
            result.add(evalDetailsWithMeta(evaluator, record));
        }
        return result;
    }

    @Override
    public boolean evaluateWithMeta(RecordEvaluatorDto evalDto, RecordAtts fullRecordMeta) {
        EvalDetails details = evalDetailsWithMeta(evalDto, fullRecordMeta);
        return details != null && details.getResult();
    }

    @Override
    public EvalDetails evalDetailsWithMeta(RecordEvaluatorDto evalDto, RecordAtts fullRecordMeta) {

        ParameterizedRecordEvaluator evaluator = this.evaluators.get(evalDto.getType());

        if (evaluator == null) {
            log.warn("Evaluator doesn't found for type " + evalDto.getType() + ". Return false.");
            return new EvalDetailsImpl();
        }

        Object config = Json.getMapper().convert(evalDto.getConfig(), evaluator.getConfigType());

        Map<String, String> metaAtts = getRequiredMetaAttributes(evalDto);

        ObjectData evaluatorMeta = ObjectData.create();
        metaAtts.forEach((k, v) -> evaluatorMeta.set(k, fullRecordMeta.getAtt(v)));

        Class<?> resMetaType = evaluator.getResMetaType();
        Object requiredMeta;
        if (resMetaType == null) {
            requiredMeta = null;
        } else if (resMetaType.isAssignableFrom(RecordMeta.class)) {
            RecordMeta meta = new RecordMeta(fullRecordMeta.getId());
            meta.setAtts(evaluatorMeta);
            requiredMeta = meta;
        } else if (resMetaType.isAssignableFrom(RecordAtts.class)) {
            RecordAtts meta = new RecordAtts(fullRecordMeta.getId());
            meta.setAtts(evaluatorMeta);
            requiredMeta = meta;
        } else {
            requiredMeta = Json.getMapper().convert(evaluatorMeta, evaluator.getResMetaType());
        }

        try {
            EvalDetails result = evaluator.evalWithDetails(requiredMeta, config);
            if (evalDto.isInverse()) {
                result = new EvalDetailsImpl(!result.getResult(), result.getCauses());
            }
            return result;
        } catch (Exception e) {
            log.error("Evaluation failed. Dto: " + evalDto + " meta: " + requiredMeta, e);
            return new EvalDetailsImpl(false, Collections.emptyList());
        }
    }

    private List<Map<String, String>> getRequiredMetaAttributes(List<RecordEvaluatorDto> evaluators) {
        List<Map<String, String>> result = new ArrayList<>();
        for (RecordEvaluatorDto dto : evaluators) {
            result.add(getRequiredMetaAttributes(dto));
        }
        return result;
    }

    @Override
    public Map<String, String> getRequiredMetaAttributes(RecordEvaluatorDto evalDto) {

        ParameterizedRecordEvaluator evaluator = this.evaluators.get(evalDto.getType());

        if (evaluator == null) {
            log.error("Evaluator with type " + evalDto.getType() + " is not found!");
            return Collections.emptyMap();
        }

        Map<String, String> attributes = null;
        try {

            Object configObj = Json.getMapper().convert(evalDto.getConfig(), evaluator.getConfigType());
            Object requiredMeta = evaluator.getMetaToRequest(configObj);

            if (requiredMeta != null) {
                if (requiredMeta instanceof Collection) {
                    @SuppressWarnings("unchecked")
                    Collection<String> typedAttributes = (Collection<String>) requiredMeta;
                    Map<String, String> attributesMap = new HashMap<>();
                    typedAttributes.forEach(att -> attributesMap.put(att, att));
                    attributes = attributesMap;
                } else if (requiredMeta instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> typedAttributes = (Map<String, String>) requiredMeta;
                    attributes = typedAttributes;
                } else if (requiredMeta instanceof Class) {
                    attributes = attSchemaWriter.writeToMap(dtoSchemaReader.read((Class<?>) requiredMeta));
                } else {
                    attributes = attSchemaWriter.writeToMap(dtoSchemaReader.read((requiredMeta.getClass())));
                }
            }
        } catch (Exception e) {
            log.error("Meta attributes can't be received. "
                + "Id: " + evalDto.getType() + " Config: " + evalDto.getConfig(), e);
        }

        if (attributes == null) {
            attributes = Collections.emptyMap();
        }

        return attributes;
    }

    @Override
    public void register(RecordEvaluator<?, ?, ?> evaluator) {

        evaluators.put(evaluator.getType(), new ParameterizedRecordEvaluator(evaluator));

        if (evaluator instanceof ServiceFactoryAware) {
            ((ServiceFactoryAware) evaluator).setRecordsServiceFactory(factory);
        }
    }

    @Override
    public void setRecordsServiceFactory(@NotNull RecordsServiceFactory factory) {
        this.factory = factory;
        recordsService = factory.getRecordsServiceV1();
        attSchemaWriter = factory.getAttSchemaWriter();
        dtoSchemaReader = factory.getDtoSchemaReader();

        register(new GroupEvaluator());
        register(new PredicateEvaluator());
        register(new AlwaysTrueEvaluator());
        register(new AlwaysFalseEvaluator());
        register(new HasAttributeEvaluator());
        register(new HasPermissionEvaluator());
    }
}
