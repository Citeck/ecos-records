package ru.citeck.ecos.records3.record.request.error;

import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.request.msg.MsgType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@MsgType("record-error")
public class RecordError {

    private String type = "";
    private String msg = "";
    private List<String> stackTrace = new ArrayList<>();
    private ObjectData data = ObjectData.create();

    public RecordError() {
    }

    public RecordError(String msg) {
        this.msg = msg;
    }

    public RecordError(String type, String msg, Object data) {
        this.type = type;
        this.msg = msg;
        this.data = ObjectData.create(data);
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<String> stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public ObjectData getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = ObjectData.create(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecordError error = (RecordError) o;
        return Objects.equals(type, error.type)
            && Objects.equals(msg, error.msg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, msg);
    }

    @Override
    public String toString() {
        return Json.getMapper().toString(this);
    }
}
