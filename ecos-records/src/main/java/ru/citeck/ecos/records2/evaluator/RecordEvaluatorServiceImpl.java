package ru.citeck.ecos.records2.evaluator;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.NullNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.*;
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

    private ObjectMapper objectMapper = new ObjectMapper();
    private RecordsServiceFactory factory;

    public RecordEvaluatorServiceImpl(RecordsServiceFactory factory) {
        this.factory = factory;
        recordsService = factory.getRecordsService();
        recordsMetaService = factory.getRecordsMetaService();
    }

    @Override
    public boolean evaluate(RecordRef recordRef, RecordEvaluatorDto evaluator) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);
        List<RecordRef> recordRefs = Collections.singletonList(recordRef);

        Map<RecordRef, List<Boolean>> evaluateResult = evaluate(recordRefs, evaluators);
        return evaluateResult.get(recordRef).get(0);
    }

    @Override
    public Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs, RecordEvaluatorDto evaluator) {

        List<RecordEvaluatorDto> evaluators = Collections.singletonList(evaluator);

        Map<RecordRef, List<Boolean>> evaluateResult = evaluate(recordRefs, evaluators);
        Map<RecordRef, Boolean> result = new HashMap<>();

        evaluateResult.forEach((ref, b) -> result.put(ref, b.get(0)));

        return result;
    }

    @Override
    public Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs, List<RecordEvaluatorDto> evaluators) {

        List<Map<String, String>> metaAttributes = getRequiredMetaAttributes(evaluators);
        Set<String> attsToRequest = new HashSet<>();

        metaAttributes.forEach(atts -> attsToRequest.addAll(atts.values()));

        List<RecordMeta> recordsMeta;
        if (!attsToRequest.isEmpty()) {
            RecordsResult<RecordMeta> recordsRes = recordsService.getAttributes(recordRefs, attsToRequest);
            recordsMeta = recordsRes.getRecords();
        } else {
            recordsMeta = recordRefs.stream().map(RecordMeta::new).collect(Collectors.toList());
        }

        Map<RecordRef, List<Boolean>> evalResultsByRecord = new HashMap<>();

        for (int i = 0; i < recordRefs.size(); i++) {
            RecordMeta meta = recordsMeta.get(i);
            List<Boolean> evalResult = evaluateWithMeta(evaluators, meta);
            evalResultsByRecord.put(recordRefs.get(i), evalResult);
        }

        return evalResultsByRecord;
    }

    private List<Boolean> evaluateWithMeta(List<RecordEvaluatorDto> evaluators, RecordMeta meta) {
        List<Boolean> result = new ArrayList<>();
        for (RecordEvaluatorDto evaluator : evaluators) {
            result.add(evaluateWithMeta(evaluator, meta));
        }
        return result;
    }

    @Override
    public boolean evaluateWithMeta(RecordEvaluatorDto evalDto, RecordMeta fullRecordMeta) {

        ParameterizedRecordEvaluator evaluator = this.evaluators.get(evalDto.getType());

        if (evaluator == null) {
            log.warn("Evaluator doesn't found for type " + evalDto.getType() + ". Return false.");
            return false;
        }

        Object config = treeToValue(evalDto.getConfig(), evaluator.getConfigType());

        Map<String, String> metaAtts = getRequiredMetaAttributes(evalDto);

        ObjectNode evaluatorMeta = JsonNodeFactory.instance.objectNode();
        metaAtts.forEach((k, v) -> {
            JsonNode value = fullRecordMeta.get(v);
            if (value.isMissingNode()) {
                value = NullNode.getInstance();
            }
            evaluatorMeta.set(k, value);
        });

        Class<?> resMetaType = evaluator.getResMetaType();
        Object requiredMeta;
        if (resMetaType == null) {
            requiredMeta = null;
        } else if (resMetaType.isAssignableFrom(RecordMeta.class)) {
            RecordMeta meta = new RecordMeta(fullRecordMeta.getId());
            meta.setAttributes(evaluatorMeta);
            requiredMeta = meta;
        } else {
            requiredMeta = treeToValue(evaluatorMeta, evaluator.getResMetaType());
        }

        try {
            boolean result = evaluator.evaluate(requiredMeta, config);
            return evalDto.isInverse() != result;
        } catch (Exception e) {
            log.error("Evaluation failed. Dto: " + evalDto + " meta: " + requiredMeta, e);
            return false;
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

            Object configObj = treeToValue(evalDto.getConfig(), evaluator.getConfigType());
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

    private Object treeToValue(JsonNode treeValue, Class<?> type) {
        Object instance = null;
        if (type != null) {
            if (type.isAssignableFrom(ObjectNode.class) && treeValue instanceof ObjectNode) {
                return treeValue.deepCopy();
            }
            try {
                instance = type.newInstance();
                treeToValue(treeValue, instance);
            } catch (Exception e) {
                log.error("Type can't be created: " + type);
                instance = null;
            }
        }
        return instance;
    }

    private void treeToValue(JsonNode treeValue, Object reqObj) {

        if (reqObj == null || treeValue == null || treeValue.isNull() || treeValue.isMissingNode()) {
            return;
        }

        try {
            objectMapper.readerForUpdating(reqObj).readValue(treeValue);
        } catch (Exception e) {
            log.error("Value conversion failed. ReqObj: " + reqObj + " value: " + treeValue, e);
        }
    }

    @Override
    public void register(RecordEvaluator<?, ?, ?> evaluator) {
        evaluators.put(evaluator.getType(), new ParameterizedRecordEvaluator(evaluator));

        if (evaluator instanceof ServiceFactoryAware) {
            ((ServiceFactoryAware) evaluator).setRecordsServiceFactory(factory);
        }
    }
}
