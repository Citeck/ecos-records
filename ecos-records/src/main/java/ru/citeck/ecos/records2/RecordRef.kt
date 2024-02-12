package ru.citeck.ecos.records2

import ecos.com.fasterxml.jackson210.annotation.JsonCreator
import ecos.com.fasterxml.jackson210.annotation.JsonValue
import ru.citeck.ecos.commons.utils.StringUtils.containsOnly
import ru.citeck.ecos.commons.utils.StringUtils.isBlank
import ru.citeck.ecos.commons.utils.StringUtils.isNotBlank
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.io.Serializable
import java.util.*

/**
 * Unique identifier of a record.
 */
@SuppressWarnings
@Deprecated(
    "Use EntityRef instead",
    ReplaceWith("EntityRef", "ru.citeck.ecos.webapp.api.entity.EntityRef")
)
class RecordRef : EntityRef, Serializable {

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
            if (sourceId != null) {
                val appDelimIdx = sourceId.indexOf(APP_NAME_DELIMITER)
                if (appDelimIdx > -1) {
                    return create(
                        sourceId.substring(0, appDelimIdx),
                        sourceId.substring(appDelimIdx + 1),
                        id
                    )
                }
            }
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
            return EntityRef.isEmpty(ref)
        }

        @JvmStatic
        @JsonCreator
        @com.fasterxml.jackson.annotation.JsonCreator
        fun valueOf(recordRefStr: String?): RecordRef {
            val ref = EntityRef.valueOf(recordRefStr)
            if (ref is RecordRef) {
                return ref
            }
            return create(ref.getAppName(), ref.getSourceId(), ref.getLocalId())
        }

        @JvmStatic
        fun valueOf(ref: RecordRef?): RecordRef {
            return ref ?: EMPTY
        }

        @JvmStatic
        fun valueOf(ref: EntityRef?): RecordRef {
            ref ?: return EMPTY
            return if (ref is RecordRef) {
                ref
            } else {
                create(ref.getAppName(), ref.getSourceId(), ref.getLocalId())
            }
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

    @JvmField
    val appName: String
    @JvmField
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

    override fun getAppName(): String {
        return appName
    }
    override fun getLocalId(): String {
        return id
    }

    override fun getSourceId(): String {
        return sourceId
    }

    fun addAppName(appName: String): RecordRef {
        return withAppName(appName)
    }

    override fun withDefault(
        appName: String,
        sourceId: String,
        localId: String
    ): RecordRef {
        val newAppName = this.appName.ifEmpty { appName }
        val newSourceId = this.sourceId.ifEmpty { sourceId }
        val newId = this.id.ifEmpty { localId }
        if (newAppName === this.appName &&
            newSourceId === this.sourceId &&
            newId === this.id
        ) {
            return this
        }
        return create(newAppName, newSourceId, newId)
    }

    override fun withDefaultSourceId(sourceId: String): RecordRef {
        if (isBlank(sourceId)) {
            return this
        }
        return if (this.sourceId.isNotEmpty()) {
            this
        } else {
            withSourceId(sourceId)
        }
    }

    override fun withDefaultAppName(appName: String): RecordRef {
        if (isBlank(appName)) {
            return this
        }
        return if (this.appName.isNotEmpty()) {
            this
        } else {
            withAppName(appName)
        }
    }

    override fun withoutAppName(): RecordRef {
        return withAppName("")
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

    override fun withSourceId(sourceId: String): RecordRef {
        val appNameDelim = sourceId.indexOf(APP_NAME_DELIMITER)
        return if (appNameDelim != -1) {
            val newAppName = sourceId.substring(0, appNameDelim)
            val newSourceId = sourceId.substring(appNameDelim + 1)
            if (this.appName == newAppName && this.sourceId == newSourceId) {
                this
            } else {
                create(newAppName, newSourceId, id)
            }
        } else {
            if (this.sourceId == sourceId) {
                this
            } else {
                create(appName, sourceId, id)
            }
        }
    }

    override fun withAppName(appName: String): RecordRef {
        if (this.appName == appName) {
            return this
        }
        return create(appName, sourceId, id)
    }

    fun withId(id: String): RecordRef {
        if (this.id == id) {
            return this
        }
        return create(appName, sourceId, id)
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
        return getAsString()
    }
}
