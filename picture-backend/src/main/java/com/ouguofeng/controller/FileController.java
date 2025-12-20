package com.ouguofeng.controller;

import com.ouguofeng.annotation.AuthCheck;
import com.ouguofeng.common.BaseResponse;

import com.ouguofeng.common.ResultUtils;
import com.ouguofeng.constant.UserConstant;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 文件上传
     *
     * @param multipartFile 文件
     * @return 存储结果
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;

        //上传文件
        try {
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            //返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("上传失败,filepath={}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                //删除临时文件
                boolean b = file.delete();
                if (!b) {
                    log.error("删除临时文件失败filepath={}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            //处理下载的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

}

