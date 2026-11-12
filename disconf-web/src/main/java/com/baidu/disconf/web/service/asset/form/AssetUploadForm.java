package com.baidu.disconf.web.service.asset.form;

import com.baidu.dsp.common.form.RequestFormBase;

import lombok.Data;

/**
 * 资产上传表单（文件本体通过 multipart 传递，见 Controller 的 file 参数）
 *
 * @author pi
 * @version 2024-11-12
 */
@Data
public class AssetUploadForm extends RequestFormBase {

    private static final long serialVersionUID = 5829473625183645912L;

    /**
     * 展示名，可选，默认使用原始文件名
     */
    private String name = "";

    /**
     * 归属 APP 名，可选
     */
    private String app = "";

    /**
     * 描述，可选
     */
    private String desc = "";
}
