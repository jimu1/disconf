package com.baidu.disconf.web.service.asset.vo;

import java.io.Serializable;

import lombok.Data;

/**
 * 资产信息返回对象
 *
 * @author pi
 * @version 2024-11-12
 */
@Data
public class AssetVo implements Serializable {

    private static final long serialVersionUID = -1093847562018347561L;

    private Long id;

    private String name;

    private String originalFileName;

    private Long fileSize;

    private String fileType;

    private String app;

    private String desc;

    private String owner;

    private String createTime;

    /**
     * 下载地址
     */
    private String downloadUrl;
}
