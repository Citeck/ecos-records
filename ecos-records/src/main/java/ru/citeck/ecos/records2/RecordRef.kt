package ru.citeck.ecos.records2

import ecos.com.fasterxml.jackson210.annotation.JsonCreator
import ecos.com.fasterxml.jackson210.annotation.JsonValue
import ru.citeck.ecos.commons.utils.StringUtils.containsOnly
import ru.citeck.ecos.commons.utils.StringUtils.isBlank
import ru.citeck.ecos.commons.utils.StringUtils.isNotBlank
import java.io.Serializable
import java.util.*

/**
 * Unique identifier of a record.
 */
class RecordRef : Serializable {

    companion object {

        private const val serialVersionUID = 1L

        @JvmField
        val EMPTY = RecordRef()

        const val SOURCE_DELIMITER = "@"
        const val APP_NAME_DELIMITER = "/"

        @JvmStatic
        fun create(sourceId: String?, id: RecordRef): RecordRef {
            return create(sourceId, id.toString())
        }

        @JvmStatic
        fun create(sourceId: String?, id: String?): RecordRef {
            return create("", sourceId, id)
        }

        @JvmStatic
        fun create(appName: String?, sourceId: String?, id: String?): RecordRef {
            return if (isBlank(appName) && isBlank(sourceId) && isBlankId(id)) {
                EMPTY
            } else RecordRef(
                appName ?: "",
                sourceId ?: "",
                (if (isBlankId(id)) "" else id)!!
            )
        }

        @JvmStatic
        fun isNotEmpty(ref: RecordRef?): Boolean {
            return !isEmpty(ref)
        }

        @JvmStatic
        fun isEmpty(ref: RecordRef?): Boolean {
            return ref == null || ref === EMPTY ||
                (
                    isBlank(ref.id) &&
                        isBlank(ref.sourceId) &&
                        isBlank(ref.appName)
                    )
        }

        @JvmStatic
        @JsonCreator
        @com.fasterxml.jackson.annotation.JsonCreator
        fun valueOf(recordRefStr: String?): RecordRef {
            if (recordRefStr == null || isBlankId(recordRefStr)) {
                return EMPTY
            }
            val id: String
            val appName: String
            val sourceId: String
            val sourceDelimIdx = recordRefStr.indexOf(SOURCE_DELIMITER)
            if (sourceDelimIdx != -1) {
                id = recordRefStr.substring(sourceDelimIdx + 1)
                val source = recordRefStr.substring(0, sourceDelimIdx)
                val appNameDelimIdx = source.indexOf(APP_NAME_DELIMITER)
                if (appNameDelimIdx > -1) {
                    appName = source.substring(0, appNameDelimIdx)
                    sourceId = source.substring(appNameDelimIdx + 1)
                } else {
                    appName = ""
                    sourceId = source
                }
            } else {
                id = recordRefStr
                sourceId = ""
                appName = ""
            }
            return create(appName, sourceId, id)
        }

        @JvmStatic
        fun valueOf(ref: RecordRef?): RecordRef {
            return ref ?: EMPTY
        }

        @JvmStatic
        private fun isBlankId(id: String?): Boolean {
            return isBlank(id) || containsOnly(id!!, '@')
        }

        @JvmStatic
        fun toString(ref: RecordRef?): String {
            return ref?.toString() ?: ""
        }
    }

    val appName: String
    val sourceId: String
    val id: String

    private constructor() {
        appName = ""
        sourceId = ""
        id = ""
    }

    private constructor(appName: String, sourceId: String, id: String) {
        this.id = if (isNotBlank(id)) id else ""
        this.appName = if (isNotBlank(appName)) appName else ""
        this.sourceId = if (isNotBlank(sourceId)) sourceId else ""
    }

    fun addAppName(appName: String): RecordRef {
        val current = toString()
        return if (current.contains("@")) {
            valueOf("$appName/$current")
        } else {
            valueOf("$appName/@$current")
        }
    }

    fun withDefaultAppName(appName: String): RecordRef {
        if (isBlank(appName)) {
            return this
        }
        return if (this.appName.isNotEmpty()) {
            this
        } else {
            addAppName(appName)
        }
    }

    fun removeAppName(): RecordRef {
        return if (appName.isEmpty()) {
            this
        } else {
            create(sourceId, id)
        }
    }

    fun isRemote(): Boolean {
        return appName.isNotEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as RecordRef
        return appName == that.appName &&
            sourceId == that.sourceId &&
            id == that.id
    }

    override fun hashCode(): Int {
        var result = Objects.hashCode(sourceId)
        result = 31 * result + Objects.hashCode(id)
        result = 31 * result + Objects.hashCode(appName)
        return result
    }

    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    override fun toString(): String {
        if (this === EMPTY) {
            return ""
        }
        if (appName.isNotEmpty()) {
            return appName + APP_NAME_DELIMITER + sourceId + SOURCE_DELIMITER + id
        }
        return if (sourceId.isNotEmpty()) {
            sourceId + SOURCE_DELIMITER + id
        } else {
            id
        }
    }
}
