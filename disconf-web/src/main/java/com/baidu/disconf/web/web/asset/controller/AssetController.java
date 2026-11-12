package com.baidu.disconf.web.web.asset.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.baidu.disconf.web.service.asset.form.AssetUploadForm;
import com.baidu.disconf.web.service.asset.service.AssetMgr;
import com.baidu.disconf.web.service.asset.vo.AssetVo;
import com.baidu.dsp.common.constant.WebConstants;
import com.baidu.dsp.common.controller.BaseController;
import com.baidu.dsp.common.vo.JsonObjectBase;

/**
 * 资产管理 Controller：支持将本地资产文件上传到服务器
 *
 * 接口列表：
 * - POST /api/web/asset/upload     上传本地资产文件（multipart/form-data）
 * - GET  /api/web/asset/list       资产列表
 * - GET  /api/web/asset/get        获取单个资产信息
 * - GET  /api/web/asset/download   下载资产文件
 * - POST /api/web/asset/delete     删除资产
 *
 * @author pi
 * @version 2024-11-12
 */
@Controller
@RequestMapping(WebConstants.API_PREFIX + "/web/asset")
public class AssetController extends BaseController {

    protected static final Logger LOG = LoggerFactory.getLogger(AssetController.class);

    @Autowired
    private AssetMgr assetMgr;

    /**
     * 上传本地资产文件
     *
     * curl 示例:
     * curl -X POST http://host:8085/api/web/asset/upload \
     *      -b "XID=xxx" \
     *      -F "file=@/path/to/local/file.zip" \
     *      -F "app=myApp" -F "desc=静态资源"
     *
     * @param form 名称/APP/描述（均可选）
     * @param file 本地文件
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public JsonObjectBase upload(AssetUploadForm form, @RequestParam("file") MultipartFile file) {

        LOG.info("asset upload: " + file.getOriginalFilename() + ", size=" + file.getSize());

        AssetVo vo = assetMgr.upload(form, file);

        return buildSuccess("asset", vo);
    }

    /**
     * 资产列表
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public JsonObjectBase list() {

        List<AssetVo> assetList = assetMgr.getAssetList();
        return buildListSuccess(assetList, assetList.size());
    }

    /**
     * 获取单个资产信息
     */
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ResponseBody
    public JsonObjectBase get(@RequestParam(value = "id") Long id) {

        return buildSuccess("asset", assetMgr.getById(id));
    }

    /**
     * 下载资产文件
     */
    @RequestMapping(value = "/download", method = RequestMethod.GET)
    @ResponseBody
    public HttpEntity<byte[]> download(@RequestParam(value = "id") Long id) {

        AssetMgr.AssetFile assetFile = assetMgr.download(id);

        HttpHeaders headers = new HttpHeaders();
        String contentType = assetFile.getAsset().getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        headers.setContentType(MediaType.parseMediaType(contentType));

        try {
            // RFC 5987 编码中文文件名
            String encodedName = URLEncoder.encode(assetFile.getAsset().getOriginalFileName(), "UTF-8")
                    .replaceAll("\\+", "%20");
            headers.add("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedName);
        } catch (UnsupportedEncodingException e) {
            headers.add("Content-Disposition", "attachment");
        }
        headers.setContentLength(assetFile.getData().length);

        return new HttpEntity<byte[]>(assetFile.getData(), headers);
    }

    /**
     * 删除资产
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public JsonObjectBase delete(@RequestParam(value = "id") Long id) {

        assetMgr.delete(id);

        return buildSuccess("删除成功");
    }
}
