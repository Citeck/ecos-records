package ru.citeck.ecos.records3.record.dao.impl.api

import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import java.nio.charset.StandardCharsets

class RecordsApiRecordsDao : AbstractRecordsDao(), RecordAttsDao {

    companion object {
        const val ID = "api"

        private val CONSUMES = Supports(
            mimetypes = listOf("application/json"),
            charsets = listOf(StandardCharsets.UTF_8.name()),
            encodings = listOf("plain")
        )

        private val PRODUCES = CONSUMES

        private val VERSION = ApiVersion(
            query = 2,
            mutate = 1,
            txn = 1,
            delete = 1
        )
    }

    override fun getRecordAtts(recordId: String): Any? {
        if (recordId != "") {
            return null
        }
        return Record(
            version = VERSION,
            consumes = CONSUMES,
            produces = PRODUCES
        )
    }

    override fun getId() = ID

    private class Record(
        val version: ApiVersion,
        val consumes: Supports,
        val produces: Supports
    )

    private class Supports(
        val mimetypes: List<String>,
        val charsets: List<String>,
        val encodings: List<String>
    )

    private class ApiVersion(
        val query: Int,
        val mutate: Int,
        val txn: Int,
        val delete: Int
    )
}
