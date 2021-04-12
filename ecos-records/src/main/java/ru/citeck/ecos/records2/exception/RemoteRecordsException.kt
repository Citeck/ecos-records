package ru.citeck.ecos.records2.exception

import ru.citeck.ecos.records3.record.request.msg.ReqMsg

class RemoteRecordsException : RecordsException {

    val errorMsg: ReqMsg

    constructor(errorMsg: ReqMsg, message: String?) : super(message) {
        this.errorMsg = errorMsg
    }
    constructor(errorMsg: ReqMsg, message: String?, cause: Throwable?) : super(message, cause) {
        this.errorMsg = errorMsg
    }
}
