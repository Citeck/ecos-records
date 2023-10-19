package ru.citeck.ecos.records3.test.evaluator;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EvaluatorsWithModelTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String ID = "test";

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);

        RecordEvaluatorService evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(new EvalutorWithModel());

        EntityRef userRef = EntityRef.create(ID, "user");
        EntityRef recordRef = EntityRef.create(ID, "record");

        Map<String, Object> model = new HashMap<>();
        model.put("user", userRef);
        model.put("fixedStrValue", ReqMetaWithModel.FIXED_STR_VALUE);
        model.put("fixedIntValue", ReqMetaWithModel.FIXED_INT_VALUE);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType(EvalutorWithModel.TYPE);

        EvalConfig config = new EvalConfig();
        config.someParam = EvalConfig.PARAM_VALUE;
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        RequestContext.doWithCtxJ(factory, data -> data.setCtxAtts(model), ctx -> {
            assertTrue(evaluatorsService.evaluate(recordRef, evaluatorDto));
            return null;
        });
    }

    @Override
    public List<?> getRecordsAtts(List<String> records) {
        return records.stream().map(r -> {
            switch (r) {
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

        @AttName("$user.userName")
        private String userName;
        private String someField;

        @AttName("$fixedStrValue")
        private String fixedStrValue;
        @AttName("$fixedIntValue")
        private int fixedIntValue;
    }
}
