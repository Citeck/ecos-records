package ru.citeck.ecos.records3.utils;

import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.JsonMapper;
import ru.citeck.ecos.records2.request.error.RecordsError;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.result.DebugResult;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.record.request.msg.ReqMsg;

import java.util.List;

public class V1ConvUtils {

    private static final JsonMapper mapper = Json.getMapper();

    public static RecordsQuery recsQueryV1ToV0(ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery query,
                                               RequestContext context) {

        RecordsQuery v0Query = new RecordsQuery();
        mapper.applyData(v0Query, query);
        if (context.isMsgEnabled(MsgLevel.DEBUG)) {
            v0Query.setDebug(true);
        }

        return v0Query;
    }

    public static ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery recsQueryV0ToV1(RecordsQuery query) {

        ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery v1Query =
            new ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery();
        mapper.applyData(v1Query, query);
        return v1Query;
    }

    public static void addErrorMessages(List<RecordsError> errors, RequestContext context) {
        if (errors != null && !errors.isEmpty()) {
            errors.forEach(e -> context.addMsg(MsgLevel.ERROR, () -> e));
        }
    }

    public static void addDebugMessage(DebugResult result, RequestContext context) {
        List<ReqMsg> messages = context.getMessages();
        if (!messages.isEmpty()) {
            ObjectData debug = ObjectData.create();
            debug.set("messages", messages);
            result.setDebug(debug);
        }
    }
}
