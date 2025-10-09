package ru.citeck.ecos.records3.record.atts.schema

enum class ScalarType(schema: String, val overridable: Boolean = true) {

    /**
     * Global identifier value. e.g. appName/localSourceId@localId
     */
    ID("id", false),

    /**
     * String value.
     */
    STR("str"),

    /**
     * Display name value.
     */
    DISP("disp"),

    /**
     * Double number value.
     */
    NUM("num"),

    /**
     * Same as ID, but used in mutation to detect linked virtual records which should be created.
     */
    ASSOC("assoc", false),

    /**
     * AppName from ID value.
     * ID: appName/localSourceId@localId <- appName - value before /.
     */
    APP_NAME("appName", false),

    /**
     * Local source ID from value.
     * ID: appName/localSourceId@localId <- localSourceId - value after / and before @.
     */
    LOCAL_SRC_ID("localSrcId", false),

    /**
     * Local ID value.
     * ID: appName/localSourceId@localId <- localId - value after @.
     */
    LOCAL_ID("localId", false),

    /**
     * Boolean value
     */
    BOOL("bool"),

    /**
     * JSON Object value.
     */
    JSON("json"),

    /**
     * Value as-is without transformations.
     * number: 123
     * string: "123"
     * bool: true
     * object: {}
     */
    RAW("raw"),

    /**
     * Binary value
     */
    BIN("bin");

    val schema = "?$schema"
    val mirrorAtt = "_$schema"

    companion object {

        /*
         * Constants required to use in annotations
         * because @AttName(SchemaType.STR.schema) doesn't work
         */
        const val ID_SCHEMA = "?id"
        const val STR_SCHEMA = "?str"
        const val DISP_SCHEMA = "?disp"
        const val NUM_SCHEMA = "?num"
        const val APP_NAME_SCHEMA = "?appName"
        const val LOCAL_ID_SCHEMA = "?localId"
        const val LOCAL_SRC_ID_SCHEMA = "?localSrcId"
        const val BOOL_SCHEMA = "?bool"
        const val JSON_SCHEMA = "?json"
        const val RAW_SCHEMA = "?raw"
        const val BIN_SCHEMA = "?bin"

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
                APP_NAME.schema -> APP_NAME
                LOCAL_SRC_ID.schema -> LOCAL_SRC_ID
                BOOL.schema -> BOOL
                JSON.schema -> JSON
                RAW.schema -> RAW
                BIN.schema -> BIN
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
                APP_NAME.mirrorAtt -> APP_NAME
                LOCAL_SRC_ID.mirrorAtt -> LOCAL_SRC_ID
                BOOL.mirrorAtt -> BOOL
                JSON.mirrorAtt -> JSON
                RAW.mirrorAtt -> RAW
                BIN.mirrorAtt -> BIN
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
