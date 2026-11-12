package com.baidu.disconf.web.service.asset.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.baidu.disconf.web.service.asset.bo.Asset;
import com.baidu.disconf.web.service.asset.form.AssetUploadForm;
import com.baidu.disconf.web.service.asset.vo.AssetVo;

/**
 * 资产管理
 *
 * @author pi
 * @version 2024-11-12
 */
public interface AssetMgr {

    /**
     * 上传本地资产文件
     *
     * @param form    表单（名称/APP/描述）
     * @param file    上传的文件
     *
     * @return 保存后的资产信息
     */
    AssetVo upload(AssetUploadForm form, MultipartFile file);

    /**
     * 资产列表
     */
    List<AssetVo> getAssetList();

    /**
     * 获取单个资产
     */
    AssetVo getById(Long id);

    /**
     * 读取资产文件内容（用于下载）
     *
     * @return 资产实体（含文件字节）
     */
    AssetFile download(Long id);

    /**
     * 删除资产（DB记录 + 本地文件）
     */
    void delete(Long id);

    /**
     * 资产文件内容封装
     */
    class AssetFile {

        private final Asset asset;
        private final byte[] data;

        public AssetFile(Asset asset, byte[] data) {
            this.asset = asset;
            this.data = data;
        }

        public Asset getAsset() {
            return asset;
        }

        public byte[] getData() {
            return data;
        }
    }
}
