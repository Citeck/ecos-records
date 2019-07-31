package ru.citeck.ecos.records2.request.error;

import java.util.List;
import java.util.Objects;

public class RecordsError {

    private String type;
    private String msg;
    private List<String> stackTrace;

    public RecordsError() {
    }

    public RecordsError(String msg) {
        this.msg = msg;
    }

    public RecordsError(String type, String msg) {
        this.type = type;
        this.msg = msg;
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

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<String> stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecordsError error = (RecordsError) o;
        return Objects.equals(type, error.type)
            && Objects.equals(msg, error.msg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, msg);
    }

    @Override
    public String toString() {
        return "RecordsError{"
            + "type='" + type + '\''
            + ", msg='" + msg + '\''
            + '}';
    }
}
