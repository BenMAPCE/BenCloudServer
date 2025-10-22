package gov.epa.bencloud;

public final class Constants {
    public static final Short SHARING_NONE = 0;
    public static final Short SHARING_ALL = 1;

    public static final String ROLE_USER = "BenMAP_Users";
    public static final String ROLE_ADMIN = "BenMAP_Admins";

    public static final String HEADER_USER_ID = "uid";
    public static final String HEADER_GROUPS = "ismemberof";
    public static final String HEADER_DISPLAY_NAME = "displayname";
    public static final String HEADER_MAIL = "mail";
    
    public static final String EPA_STANDARD_VALUATION = "Use EPA's current default values";
    
    public static final String FILE_TYPE_GRID = "GRID";
    public static final String FILE_TYPE_AQ = "AQ";
    public static final String FILE_TYPE_RESULT_EXPORT = "RESEXP";
    
    public static final String  TASK_TYPE_HIF = "HIF";
    public static final String  TASK_TYPE_VALUATION = "Valuation";
    public static final String  TASK_TYPE_EXPOSURE = "Exposure";
    public static final String  TASK_TYPE_GRID_IMPORT = "Grid Import";
    public static final String  TASK_TYPE_AQ_IMPORT = "AQ Import";
    public static final String  TASK_TYPE_RESULT_EXPORT = "Result Export";
    
}
