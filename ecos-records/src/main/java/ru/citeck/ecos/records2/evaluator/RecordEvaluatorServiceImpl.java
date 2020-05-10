package ru.citeck.ecos.records2.evaluator;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.evaluator.details.EvalDetails;
import ru.citeck.ecos.records2.evaluator.details.EvalDetailsImpl;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class RecordEvaluatorServiceImpl implements RecordEvaluatorService {

    private RecordsService recordsService;
    private RecordsMetaService recordsMetaService;

    private Map<String, ParameterizedRecordEvaluator> evaluators = new ConcurrentHashMap<>();

    private RecordsServiceFactory factory;

    public RecordEvaluatorServiceImpl(RecordsServiceFactory factory) {
        this.factory = factory;
        recordsService = factory.getRecordsService();
        recordsMetaService = factory.getRecordsMetaService();
    }

    @Override
    public boolean evaluate(RecordRef recordRef, RecordEvaluatorDto evaluator) {
        return evaluate(recordRef, evaluator, Collections.emptyMap());
    }

    @Override
    public boolean evaluate(RecordRef recordRef,
                            RecordEvaluatorDto evaluator,
                            Object model) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);
        List<RecordRef> recordRefs = Collections.singletonList(recordRef);

        Map<RecordRef, List<Boolean>> evaluateResult = evaluate(recordRefs, evaluators, model);
        return evaluateResult.get(recordRef).get(0);
    }

    @Override
    public Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs, RecordEvaluatorDto evaluator) {
        return evaluate(recordRefs, evaluator, Collections.emptyMap());
    }



    @Override
    public Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs,
                                            RecordEvaluatorDto evaluator,
                                            Object model) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);

        Map<RecordRef, List<Boolean>> evaluateResult = evaluate(recordRefs, evaluators, model);
        Map<RecordRef, Boolean> result = new HashMap<>();

        evaluateResult.forEach((ref, b) -> result.put(ref, b.get(0)));

        return result;
    }

    @Override
    public Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs,
                                                  List<RecordEvaluatorDto> evaluators) {
        return evaluate(recordRefs, evaluators, Collections.emptyMap());
    }

    @Override
    public Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs,
                                                  List<RecordEvaluatorDto> evaluators,
                                                  Object model) {

        Map<RecordRef, List<EvalDetails>> details = evalWithDetails(recordRefs, evaluators, model);
        Map<RecordRef, List<Boolean>> result = new HashMap<>();

        details.forEach((k, v) -> {
            List<Boolean> resultsList = result.computeIfAbsent(k, kk -> new ArrayList<>());
            v.forEach(d -> resultsList.add(d.getResult()));
        });

        return result;
    }

    @Override
    public EvalDetails evalWithDetails(RecordRef recordRef,
                                       RecordEvaluatorDto evaluator) {
        return evalWithDetails(recordRef, evaluator, Collections.emptyMap());
    }

    @Override
    public EvalDetails evalWithDetails(RecordRef recordRef,
                                       RecordEvaluatorDto evaluator,
                                       Object model) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);
        List<RecordRef> recordRefs = Collections.singletonList(recordRef);

        Map<RecordRef, List<EvalDetails>> evaluateResult = evalWithDetails(recordRefs, evaluators, model);
        return evaluateResult.get(recordRef).get(0);
    }


    @Override
    public Map<RecordRef, EvalDetails> evalWithDetails(List<RecordRef> recordRefs,
                                                       RecordEvaluatorDto evaluator) {
        return evalWithDetails(recordRefs, evaluator, Collections.emptyMap());
    }

    @Override
    public Map<RecordRef, EvalDetails> evalWithDetails(List<RecordRef> recordRefs,
                                                       RecordEvaluatorDto evaluator,
                                                       Object model) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);

        Map<RecordRef, List<EvalDetails>> evaluateResult = evalWithDetails(recordRefs, evaluators, model);
        Map<RecordRef, EvalDetails> result = new HashMap<>();

        evaluateResult.forEach((ref, b) -> result.put(ref, b.get(0)));

        return result;
    }

    @Override
    public Map<RecordRef, List<EvalDetails>> evalWithDetails(List<RecordRef> recordRefs,
                                                             List<RecordEvaluatorDto> evaluators) {

        return evalWithDetails(recordRefs, evaluators, Collections.emptyMap());
    }

    @Override
    public Map<RecordRef, List<EvalDetails>> evalWithDetails(List<RecordRef> recordRefs,
                                                             List<RecordEvaluatorDto> evaluators,
                                                             Object model) {

        if (model == null) {
            model = Collections.emptyMap();
        }

        List<Map<String, String>> metaAttributes = getRequiredMetaAttributes(evaluators);
        Set<String> attsToRequest = new HashSet<>();

        metaAttributes.forEach(atts -> attsToRequest.addAll(atts.values()));

        Set<String> recordAttsToRequest = new HashSet<>();
        Map<String, String> modelAttsToRequest = new HashMap<>();

        attsToRequest.forEach(att -> {
            if (att.charAt(0) == '$' || att.startsWith(".att(n:\"$") || att.startsWith(".atts(n:\"$")) {
                modelAttsToRequest.put(att, att.replaceFirst("\\$", ""));
            } else {
                recordAttsToRequest.add(att);
            }
        });

        RecordMeta modelMeta = null;
        if (!modelAttsToRequest.isEmpty()) {
            modelMeta = recordsMetaService.getMeta(model, modelAttsToRequest);
        }

        List<RecordMeta> recordsMeta;
        if (!attsToRequest.isEmpty()) {
            RecordsResult<RecordMeta> recordsRes = recordsService.getAttributes(recordRefs, recordAttsToRequest);
            recordsMeta = recordsRes.getRecords();
        } else {
            recordsMeta = recordRefs.stream().map(RecordMeta::new).collect(Collectors.toList());
        }

        Map<RecordRef, List<EvalDetails>> evalResultsByRecord = new HashMap<>();

        for (int i = 0; i < recordRefs.size(); i++) {
            RecordMeta meta = recordsMeta.get(i);
            List<EvalDetails> evalResult = evaluateWithMeta(evaluators, meta, modelMeta);
            evalResultsByRecord.put(recordRefs.get(i), evalResult);
        }

        return evalResultsByRecord;
    }

    private List<EvalDetails> evaluateWithMeta(List<RecordEvaluatorDto> evaluators,
                                               RecordMeta record,
                                               RecordMeta model) {

        List<EvalDetails> result = new ArrayList<>();
        for (RecordEvaluatorDto evaluator : evaluators) {
            if (model != null && model.getAttributes().size() != 0) {
                RecordMeta recordWithModel = new RecordMeta(record);
                model.forEach(recordWithModel::set);
                record = recordWithModel;
            }
            result.add(evalDetailsWithMeta(evaluator, record));
        }
        return result;
    }

    @Override
    public boolean evaluateWithMeta(RecordEvaluatorDto evalDto, RecordMeta fullRecordMeta) {
        EvalDetails details = evalDetailsWithMeta(evalDto, fullRecordMeta);
        return details != null && details.getResult();
    }

    @Override
    public EvalDetails evalDetailsWithMeta(RecordEvaluatorDto evalDto, RecordMeta fullRecordMeta) {

        ParameterizedRecordEvaluator evaluator = this.evaluators.get(evalDto.getType());

        if (evaluator == null) {
            log.warn("Evaluator doesn't found for type " + evalDto.getType() + ". Return false.");
            return new EvalDetailsImpl();
        }

        Object config = Json.getMapper().convert(evalDto.getConfig(), evaluator.getConfigType());

        Map<String, String> metaAtts = getRequiredMetaAttributes(evalDto);

        ObjectData evaluatorMeta = ObjectData.create();
        metaAtts.forEach((k, v) -> evaluatorMeta.set(k, fullRecordMeta.get(v)));

        Class<?> resMetaType = evaluator.getResMetaType();
        Object requiredMeta;
        if (resMetaType == null) {
            requiredMeta = null;
        } else if (resMetaType.isAssignableFrom(RecordMeta.class)) {
            RecordMeta meta = new RecordMeta(fullRecordMeta.getId());
            meta.setAttributes(evaluatorMeta);
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
                    attributes = recordsMetaService.getAttributes((Class<?>) requiredMeta);
                } else {
                    attributes = recordsMetaService.getAttributes(requiredMeta.getClass());
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
}
