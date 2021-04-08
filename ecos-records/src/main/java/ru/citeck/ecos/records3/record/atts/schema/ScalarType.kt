package ru.citeck.ecos.records3.record.atts.schema

enum class ScalarType(val schema: String, val overridable: Boolean) {

    /**
     * Global identifier value. e.g. appName/localSourceId@localId
     */
    ID("?id", false),

    /**
     * String value.
     */
    STR("?str", true),

    /**
     * Display name value.
     */
    DISP("?disp", true),

    /**
     * Double number value.
     */
    NUM("?num", true),

    /**
     * Same as ID, but used in mutation to detect linked virtual records which should be created.
     */
    ASSOC("?assoc", true),

    /**
     * Local ID value.
     * ID: appName/localSourceId@localId <- localId - value after @.
     */
    LOCAL_ID("?localId", false),

    /**
     * Boolean value
     */
    BOOL("?bool", true),

    /**
     * JSON Object value.
     */
    JSON("?json", true);

    val mirrorAtt = schema.replaceFirst('?', '_')

    companion object {

        @JvmStatic
        fun getBySchema(schema: String): ScalarType? {
            if (schema.isEmpty() || schema[0] != '?') {
                return null
            }
            return when (schema) {
                ID.schema -> ID
                STR.schema -> STR
                DISP.schema -> DISP
                NUM.schema -> NUM
                ASSOC.schema -> ASSOC
                LOCAL_ID.schema -> LOCAL_ID
                BOOL.schema -> BOOL
                JSON.schema -> JSON
                else -> null
            }
        }

        @JvmStatic
        fun getByMirrorAtt(att: String): ScalarType? {
            if (att.isEmpty() || att[0] != '_') {
                return null
            }
            return when (att) {
                ID.mirrorAtt -> ID
                STR.mirrorAtt -> STR
                DISP.mirrorAtt -> DISP
                NUM.mirrorAtt -> NUM
                ASSOC.mirrorAtt -> ASSOC
                LOCAL_ID.mirrorAtt -> LOCAL_ID
                BOOL.mirrorAtt -> BOOL
                JSON.mirrorAtt -> JSON
                else -> null
            }
        }

        @JvmStatic
        fun getBySchemaOrMirrorAtt(schemaOrAtt: String?): ScalarType? {
            if (schemaOrAtt.isNullOrBlank()) {
                return null
            }
            if (schemaOrAtt[0] == '_') {
                return getByMirrorAtt(schemaOrAtt)
            } else if (schemaOrAtt[0] == '?') {
                return getBySchema(schemaOrAtt)
            }
            return null
        }
    }
}
