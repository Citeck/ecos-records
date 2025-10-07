package ru.citeck.ecos.records3.record.dao.impl.ext

class ExtStorageRecordsDaoConfig<T : Any>(
    val sourceId: String,
    val storage: ExtStorage<T>,
    val ecosType: String,
    val workspaceScoped: Boolean
) {
    companion object {
        @JvmStatic
        fun <T : Any> create(storage: ExtStorage<T>): Builder<T> {
            return Builder(storage)
        }
    }

    class Builder<T : Any>(
        private val storage: ExtStorage<T>
    ) {
        private var sourceId: String = ""
        private var ecosType: String = ""
        private var workspaceScoped: Boolean = false

        fun withSourceId(sourceId: String?): Builder<T> {
            this.sourceId = sourceId ?: ""
            return this
        }

        fun withEcosType(ecosType: String?): Builder<T> {
            this.ecosType = ecosType ?: ""
            return this
        }

        fun withWorkspaceScoped(workspaceScoped: Boolean?): Builder<T> {
            this.workspaceScoped = workspaceScoped ?: false
            return this
        }

        fun build(): ExtStorageRecordsDaoConfig<T> {
            if (sourceId.isBlank()) {
                error("sourceId is blank")
            }
            return ExtStorageRecordsDaoConfig(sourceId, storage, ecosType, workspaceScoped)
        }
    }
}
