package com.ouguofeng.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 发布图片到公共空间请求
 */
@Data
public class PicturePublishToPublicRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
