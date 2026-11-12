package com.baidu.disconf.web.service.asset.bo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.dsp.common.dao.Columns;
import com.baidu.dsp.common.dao.DB;
import com.baidu.unbiz.common.genericdao.annotation.Column;
import com.baidu.unbiz.common.genericdao.annotation.Table;
import com.github.knightliao.apollo.db.bo.BaseObject;

/**
 * 资产实体：记录上传到服务器的本地资产文件元信息，
 * 文件实体保存在本地磁盘（见 AssetMgr 配置的目录）
 *
 * @author pi
 * @version 2024-11-12
 */
@Table(db = DB.DB_NAME, name = "asset", keyColumn = Columns.ASSET_ID)
public class Asset extends BaseObject<Long> {

    protected static final Logger LOG = LoggerFactory.getLogger(Asset.class);

    private static final long serialVersionUID = 4728503612826331664L;

    /**
     * 展示名（可自定义，默认为原始文件名）
     */
    @Column(value = Columns.NAME)
    private String name;

    /**
     * 用户上传时的原始文件名
     */
    @Column(value = Columns.ASSET_ORIGINAL_FILE_NAME)
    private String originalFileName;

    /**
     * 服务器上实际存储的文件名（服务端生成，UUID + 后缀，不含任何用户输入路径信息）
     */
    @Column(value = Columns.ASSET_STORED_NAME)
    private String storedName;

    /**
     * 文件大小（字节）
     */
    @Column(value = Columns.ASSET_FILE_SIZE)
    private Long fileSize;

    /**
     * 文件后缀（如 .zip，小写）
     */
    @Column(value = Columns.ASSET_FILE_TYPE)
    private String fileType;

    /**
     * MIME 类型
     */
    @Column(value = Columns.ASSET_CONTENT_TYPE)
    private String contentType;

    /**
     * 归属的 APP 名（可空）
     */
    @Column(value = Columns.ASSET_APP)
    private String app;

    /**
     * 描述
     */
    @Column(value = Columns.ASSET_DESC)
    private String desc;

    /**
     * 上传者
     */
    @Column(value = Columns.ASSET_OWNER)
    private String owner;

    /**
     * 创建时间
     */
    @Column(value = Columns.CREATE_TIME)
    private String createTime;

    /**
     * 更新时间
     */
    @Column(value = Columns.UPDATE_TIME)
    private String updateTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "Asset [name=" + name + ", originalFileName=" + originalFileName + ", storedName=" + storedName +
                   ", fileSize=" + fileSize + ", fileType=" + fileType + ", app=" + app + ", owner=" + owner +
                   ", createTime=" + createTime + ", updateTime=" + updateTime + "]";
    }
}
