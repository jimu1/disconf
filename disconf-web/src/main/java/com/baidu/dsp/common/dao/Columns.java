package com.baidu.dsp.common.dao;

/**
 * @author liaoqiqi
 * @version 2014-1-14
 */
public class Columns {

    public static final String CREATE_TIME = "create_time";
    public static final String UPDATE_TIME = "update_time";
    public static final String CREATE_BY = "create_by";
    public static final String UPDATE_BY = "update_by";

    public static final String NAME = "name";

    public static final String VERSION = "version";

    public static final String DESC = "description";

    public static final String VALUE = "value";

    public static final String APP_ID = "app_id";
    public static final String ENV_ID = "env_id";
    public static final String CONFIG_ID = "config_id";
    public static final String USER_ID = "user_id";

    public static final String TYPE = "type";

    public static final String STATUS = "status";

    public static final String TOKEN = "token";
    public static final String PASSWORD = "password";

    public static final String EMAILS = "emails";

    public static final String OWNAPPS = "ownapps";

    public static final String ROLE_ID = "role_id";

    public static final class RoleColumns {
        public final static String ROLE_NAME = "role_name";
    }

    /**
     * 资产(asset)表字段
     */
    public static final String ASSET_ID = "asset_id";
    public static final String ASSET_ORIGINAL_FILE_NAME = "original_file_name";
    public static final String ASSET_STORED_NAME = "stored_name";
    public static final String ASSET_FILE_SIZE = "file_size";
    public static final String ASSET_FILE_TYPE = "file_type";
    public static final String ASSET_CONTENT_TYPE = "content_type";
    public static final String ASSET_APP = "asset_app";
    public static final String ASSET_DESC = "asset_desc";
    public static final String ASSET_OWNER = "asset_owner";
}
