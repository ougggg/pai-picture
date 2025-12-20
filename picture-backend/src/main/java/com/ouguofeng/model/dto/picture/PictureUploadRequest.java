package com.ouguofeng.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 接收图片上传的封装类
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片id
      */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;  
}