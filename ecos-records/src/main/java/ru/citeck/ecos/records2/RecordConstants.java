package ru.citeck.ecos.records2;

public final class RecordConstants {

    private RecordConstants() {}

    public static final String ATT_PARENT = "_parent";
    public static final String ATT_PARENT_ATT = "_parentAtt";
    public static final String ATT_TYPE = "_type";
    public static final String ATT_ECOS_TYPE = "_etype";
    public static final String ATT_FORM_KEY = "_formKey";
    public static final String ATT_FORM_MODE = "_formMode";
    public static final String ATT_ALIAS = "_alias";
    public static final String ATT_DASHBOARD_KEY = "_dashboardKey";
    public static final String ATT_DASHBOARD_TYPE = "_dashboardType";
    public static final String ATT_MODIFIED = "_modified";
    public static final String ATT_MODIFIER = "_modifier";
    public static final String ATT_CONTENT = "_content";
    public static final String ATT_ACTIONS = "_actions";
    public static final String ATT_NOT_EXISTS = "_notExists";

    public static final String FORM_MODE_CREATE = "CREATE";
    public static final String FORM_MODE_EDIT = "EDIT";

    /**
     * Deprecated attribute.
     * @deprecated view form defined by mode. Not by this attribute
     */
    @Deprecated
    public static final String ATT_VIEW_FORM_KEY = "_viewFormKey";
}
