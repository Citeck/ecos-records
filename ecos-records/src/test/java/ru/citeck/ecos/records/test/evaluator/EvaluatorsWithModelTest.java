package ru.citeck.ecos.records.test.evaluator;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EvaluatorsWithModelTest extends LocalRecordsDao implements LocalRecordsMetaDao {

    private static final String ID = "test";

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);

        RecordEvaluatorService evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(new EvalutorWithModel());

        RecordRef userRef = RecordRef.create(ID, "user");
        RecordRef recordRef = RecordRef.create(ID, "record");

        Map<String, Object> model = new HashMap<>();
        model.put("user", userRef);
        model.put("fixedStrValue", ReqMetaWithModel.FIXED_STR_VALUE);
        model.put("fixedIntValue", ReqMetaWithModel.FIXED_INT_VALUE);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType(EvalutorWithModel.TYPE);

        EvalConfig config = new EvalConfig();
        config.someParam = EvalConfig.PARAM_VALUE;
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        assertTrue(evaluatorsService.evaluate(recordRef, evaluatorDto, model));
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream().map(r -> {
            switch (r.getId()) {
                case "user": return new UserValue();
                case "record": return new OtherValue();
            }
            throw new RuntimeException("unknown record: " + r);
        }).collect(Collectors.toList());
    }

    @Data
    public static class UserValue {

        public static final String USER_NAME = "Some User Name";
        public static final Boolean IS_ADMIN = true;

        private String userName = USER_NAME;
        private boolean isAdmin = IS_ADMIN;
    }

    @Data
    public static class OtherValue {

        public static final String SOME_FIELD_VALUE = "Some field value";

        private String someField = SOME_FIELD_VALUE;
    }

    public static class EvalutorWithModel implements RecordEvaluator<Class<ReqMetaWithModel>, ReqMetaWithModel, EvalConfig> {

        public static final String TYPE = "test-eval-type";

        @Override
        public Class<ReqMetaWithModel> getMetaToRequest(EvalConfig config) {
            return ReqMetaWithModel.class;
        }

        @Override
        public boolean evaluate(ReqMetaWithModel meta, EvalConfig config) {
            return config.someParam.equals(EvalConfig.PARAM_VALUE)
                && meta.userName.equals(UserValue.USER_NAME)
                && meta.someField.equals(OtherValue.SOME_FIELD_VALUE)
                && meta.fixedStrValue.equals(ReqMetaWithModel.FIXED_STR_VALUE)
                && meta.fixedIntValue == ReqMetaWithModel.FIXED_INT_VALUE;
        }

        @Override
        public String getType() {
            return TYPE;
        }
    }

    @Data
    public static class EvalConfig {

        public static final String PARAM_VALUE = "param value";

        private String someParam;
    }

    @Data
    public static class ReqMetaWithModel {

        public static final String FIXED_STR_VALUE = "Fixed str value";
        public static final int FIXED_INT_VALUE = 42;

        @MetaAtt("$user.userName")
        private String userName;
        private String someField;

        @MetaAtt("$fixedStrValue")
        private String fixedStrValue;
        @MetaAtt("$fixedIntValue")
        private int fixedIntValue;
    }
}
