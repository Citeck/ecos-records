package ru.citeck.ecos.records2.request.error;

public class RecordsError {

    private String msg;

    public RecordsError() {
    }

    public RecordsError(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
