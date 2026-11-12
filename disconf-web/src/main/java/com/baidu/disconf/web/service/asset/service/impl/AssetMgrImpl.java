package com.baidu.disconf.web.service.asset.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baidu.disconf.web.service.asset.bo.Asset;
import com.baidu.disconf.web.service.asset.dao.AssetDao;
import com.baidu.disconf.web.service.asset.form.AssetUploadForm;
import com.baidu.disconf.web.service.asset.service.AssetMgr;
import com.baidu.disconf.web.service.asset.vo.AssetVo;
import com.baidu.disconf.web.service.user.dto.Visitor;
import com.baidu.dsp.common.constant.DataFormatConstants;
import com.baidu.dsp.common.exception.DocumentNotFoundException;
import com.baidu.dsp.common.exception.FileUploadException;
import com.baidu.ub.common.commons.ThreadContext;
import com.github.knightliao.apollo.utils.time.DateUtils;

/**
 * 资产管理实现：文件实体保存在本地磁盘，元信息保存于 asset 表
 *
 * @author pi
 * @version 2024-11-12
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
public class AssetMgrImpl implements AssetMgr {

    protected static final Logger LOG = LoggerFactory.getLogger(AssetMgrImpl.class);

    /**
     * 本地资产保存目录，可通过 application.properties 中 disconf.web.asset.dir 配置，默认 asset/
     */
    @Value("${disconf.web.asset.dir:asset}")
    private String assetDir;

    /**
     * 单个资产文件的大小上限（字节），默认 32MB
     */
    @Value("${disconf.web.asset.max.size:33554432}")
    private long maxAssetSize;

    @Autowired
    private AssetDao assetDao;

    /**
     * 上传：本地文件 -> 服务器磁盘 + DB 元信息
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public AssetVo upload(AssetUploadForm form, MultipartFile file) {

        // 基本校验
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("上传文件不能为空");
        }
        if (file.getSize() > maxAssetSize) {
            throw new FileUploadException("文件大小超过上限: " + maxAssetSize + " 字节");
        }

        // 原始文件名仅取最后的文件名部分，防止路径信息注入
        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        if (originalFileName.isEmpty()) {
            throw new FileUploadException("文件名不能为空");
        }

        String ext = FilenameUtils.getExtension(originalFileName).toLowerCase();

        // 服务端生成存储文件名，与用户输入完全无关，杜绝路径穿越
        String storedName = UUID.randomUUID().toString().replaceAll("-", "") +
                (ext.isEmpty() ? "" : "." + ext);

        // 保存到本地磁盘
        File dir = getStorageDir();
        File dest = new File(dir, storedName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            LOG.error("save asset file error: " + e.toString());
            throw new FileUploadException("保存文件失败", e);
        }

        // 入库
        Asset asset = new Asset();
        asset.setName(pickDisplayName(form, originalFileName));
        asset.setOriginalFileName(originalFileName);
        asset.setStoredName(storedName);
        asset.setFileSize(file.getSize());
        asset.setFileType(ext.isEmpty() ? "" : "." + ext);
        String contentType = file.getContentType();
        asset.setContentType(contentType == null ? "application/octet-stream" : contentType);
        asset.setApp(emptyToNull(form.getApp()));
        asset.setDesc(emptyToNull(form.getDesc()));
        asset.setOwner(getCurrentUserName());

        String curTime = DateUtils.format(new Date(), DataFormatConstants.COMMON_TIME_FORMAT);
        asset.setCreateTime(curTime);
        asset.setUpdateTime(curTime);

        asset = assetDao.create(asset);

        LOG.info("asset uploaded: " + asset.toString());

        return toVo(asset);
    }

    @Override
    public List<AssetVo> getAssetList() {

        List<AssetVo> vos = new ArrayList<AssetVo>();
        List<Asset> assets = assetDao.findAll();
        for (Asset asset : assets) {
            vos.add(toVo(asset));
        }
        return vos;
    }

    @Override
    public AssetVo getById(Long id) {

        Asset asset = assetDao.get(id);
        if (asset == null) {
            throw new DocumentNotFoundException("asset " + id);
        }
        return toVo(asset);
    }

    @Override
    public AssetFile download(Long id) {

        Asset asset = assetDao.get(id);
        if (asset == null) {
            throw new DocumentNotFoundException("asset " + id);
        }

        File file = new File(getStorageDir(), asset.getStoredName());
        if (!file.exists() || !file.isFile()) {
            throw new DocumentNotFoundException(asset.getStoredName());
        }

        byte[] data;
        try {
            data = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            LOG.error("read asset file error: " + e.toString());
            throw new FileUploadException("读取文件失败", e);
        }

        return new AssetFile(asset, data);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void delete(Long id) {

        Asset asset = assetDao.get(id);
        if (asset == null) {
            throw new DocumentNotFoundException("asset " + id);
        }

        assetDao.delete(id);

        // DB 删除成功后再清理磁盘文件，失败仅告警
        File file = new File(getStorageDir(), asset.getStoredName());
        if (file.exists() && !file.delete()) {
            LOG.warn("cannot delete asset file on disk: " + asset.getStoredName());
        }
    }

    // ------------------- 私有方法 -------------------

    /**
     * 本地存储目录，不存在则自动创建
     */
    private File getStorageDir() {

        File dir = new File(assetDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new FileUploadException("无法创建资产存储目录: " + assetDir);
        }
        return dir;
    }

    /**
     * 清理文件名：去掉所有路径部分，仅保留文件名本身
     */
    private String sanitizeFileName(String fileName) {

        if (fileName == null) {
            return "";
        }
        String name = fileName.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        // 去掉潜在的目录穿越字符与控制字符
        name = name.replaceAll("[\\r\\n\\t\\0]", "");
        return name.trim();
    }

    private String pickDisplayName(AssetUploadForm form, String originalFileName) {

        String name = form.getName();
        if (name == null || name.trim().isEmpty()) {
            return originalFileName;
        }
        return sanitizeFileName(name);
    }

    private String emptyToNull(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        return s.trim();
    }

    private String getCurrentUserName() {

        try {
            Visitor visitor = ThreadContext.getSessionVisitor();
            if (visitor != null && visitor.getLoginUserName() != null) {
                return visitor.getLoginUserName();
            }
        } catch (Exception e) {
            LOG.warn("cannot get current user: " + e.toString());
        }
        return "unknown";
    }

    private AssetVo toVo(Asset asset) {

        AssetVo vo = new AssetVo();
        vo.setId(asset.getId());
        vo.setName(asset.getName());
        vo.setOriginalFileName(asset.getOriginalFileName());
        vo.setFileSize(asset.getFileSize());
        vo.setFileType(asset.getFileType());
        vo.setApp(asset.getApp());
        vo.setDesc(asset.getDesc());
        vo.setOwner(asset.getOwner());
        vo.setCreateTime(asset.getCreateTime());
        vo.setDownloadUrl("/api/web/asset/download?id=" + asset.getId());
        return vo;
    }
}
