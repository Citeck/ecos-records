package ru.citeck.ecos.records2.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.evaluators.*;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class RecordEvaluatorsServiceImpl implements RecordEvaluatorsService {

    private RecordsService recordsService;
    private RecordsMetaService recordsMetaService;

    private Map<String, RecordEvaluator> evaluators = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    public RecordEvaluatorsServiceImpl(RecordsServiceFactory factory) {
        recordsService = factory.getRecordsService();
        recordsMetaService = factory.getRecordsMetaService();

        register(new AlwaysTrueEvaluator());
        register(new AlwaysFalseEvaluator());
        register(new HasAttributeEvaluator());
        register(new HasPermissionEvaluator());
        register(new GroupEvaluator());
    }

    @Override
    public boolean evaluate(RecordRef recordRef, EvaluatorDto evaluator) {

        List<EvaluatorDto> evaluators = Collections.singletonList(evaluator);
        List<RecordRef> recordRefs = Collections.singletonList(recordRef);

        Map<RecordRef, List<Boolean>> evaluateResult = evaluate(recordRefs, evaluators);
        return evaluateResult.get(recordRef).get(0);
    }

    @Override
    public Map<RecordRef, Boolean> evaluate(List<RecordRef> recordRefs, EvaluatorDto evaluator) {

        List<EvaluatorDto> evaluators = Collections.singletonList(evaluator);

        Map<RecordRef, List<Boolean>> evaluateResult = evaluate(recordRefs, evaluators);
        Map<RecordRef, Boolean> result = new HashMap<>();

        evaluateResult.forEach((ref, b) -> result.put(ref, b.get(0)));

        return result;
    }

    @Override
    public Map<RecordRef, List<Boolean>> evaluate(List<RecordRef> recordRefs, List<EvaluatorDto> evaluators) {

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
            List<ObjectNode> evaluatorsMeta = new ArrayList<>();

            for (Map<String, String> metaAtts : metaAttributes) {
                ObjectNode evalMeta = JsonNodeFactory.instance.objectNode();
                metaAtts.forEach((k, v) -> {
                    JsonNode value = meta.get(v);
                    if (value.isMissingNode()) {
                        value = NullNode.getInstance();
                    }
                    evalMeta.set(k, value);
                });
                evaluatorsMeta.add(evalMeta);
            }

            List<Boolean> evalResult = evaluateWithMeta(evaluators, evaluatorsMeta);
            evalResultsByRecord.put(recordRefs.get(i), evalResult);
        }

        return evalResultsByRecord;
    }

    private List<Boolean> evaluateWithMeta(List<EvaluatorDto> evaluators, List<ObjectNode> meta) {
        List<Boolean> result = new ArrayList<>();
        for (int i = 0; i < evaluators.size(); i++) {
            result.add(evaluateWithMeta(evaluators.get(i), meta.get(i)));
        }
        return result;
    }

    @Override
    public boolean evaluateWithMeta(EvaluatorDto evalDto, ObjectNode metaNode) {

        @SuppressWarnings("unchecked")
        RecordEvaluator<Object, Object, Object> evaluator =
            (RecordEvaluator<Object, Object, Object>) this.evaluators.get(evalDto.getType());

        if (evaluator == null) {
            return false;
        }

        Object config = treeToValue(evalDto.getConfig(), evaluator.getConfigType());
        Object requiredMeta = treeToValue(metaNode, evaluator.getEvalMetaType());

        try {
            boolean result = evaluator.evaluate(requiredMeta, config);
            return evalDto.isInverse() != result;
        } catch (Exception e) {
            log.error("Evaluation failed. Dto: " + evalDto + " meta: " + metaNode, e);
            return false;
        }
    }

    private List<Map<String, String>> getRequiredMetaAttributes(List<EvaluatorDto> evaluators) {
        List<Map<String, String>> result = new ArrayList<>();
        for (EvaluatorDto dto : evaluators) {
            result.add(getRequiredMetaAttributes(dto));
        }
        return result;
    }

    @Override
    public Map<String, String> getRequiredMetaAttributes(EvaluatorDto evalDto) {

        @SuppressWarnings("unchecked")
        RecordEvaluator<Object, Object, Object> evaluator =
            (RecordEvaluator<Object, Object, Object>) this.evaluators.get(evalDto.getType());

        if (evaluator == null) {
            log.error("Evaluator with type " + evalDto.getType() + " is not found!");
            return Collections.emptyMap();
        }

        Map<String, String> attributes = null;
        try {

            Object configObj = treeToValue(evalDto.getConfig(), evaluator.getConfigType());
            Object requiredMeta = evaluator.getRequiredMeta(configObj);

            if (requiredMeta != null) {
                if (requiredMeta instanceof Map) {
                    attributes = (Map<String, String>) requiredMeta;
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

    private Object treeToValue(JsonNode treeValue, Class<Object> type) {
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
        evaluators.put(evaluator.getType(), evaluator);

        if (evaluator instanceof RecordEvaluatorsServiceAware) {
            ((RecordEvaluatorsServiceAware) evaluator).setRecordEvaluatorsService(this);
        }
    }
}
