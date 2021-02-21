package ru.citeck.ecos.records2

object RecordConstants {

    const val ATT_NULL = "_null"

    const val ATT_TYPE = "_type"
    @Deprecated(message = "Use ATT_TYPE instead", replaceWith = ReplaceWith("ATT_TYPE"))
    const val ATT_ECOS_TYPE = "_etype"

    const val ATT_NOT_EXISTS = "_notExists"
    const val ATT_LOCAL_ID = "_localId"

    const val ATT_AS = "_as"
    const val ATT_HAS = "_has"
    const val ATT_EDGE = "_edge"

    const val ATT_ALIAS = "_alias"

    const val ATT_PARENT = "_parent"
    const val ATT_PARENT_ATT = "_parentAtt"
    const val ATT_FORM_KEY = "_formKey"
    const val ATT_FORM_REF = "_formRef"
    const val ATT_FORM_MODE = "_formMode"

    const val ATT_MODIFIED = "_modified"
    const val ATT_MODIFIER = "_modifier"
    const val ATT_CREATED = "_created"
    const val ATT_CREATOR = "_creator"

    const val ATT_CONTENT = "_content"
    const val ATT_ACTIONS = "_actions"
    const val ATT_DOC_NUM = "_docNum"

    const val ATT_DISP = "_disp"
    const val ATT_STR = "_str"

    const val FORM_MODE_CREATE = "CREATE"
    const val FORM_MODE_EDIT = "EDIT"

    const val LANG_COLUMNS_META = "columns-meta"
}
