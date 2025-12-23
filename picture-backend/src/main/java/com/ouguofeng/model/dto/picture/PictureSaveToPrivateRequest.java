package com.ouguofeng.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 保存图片到私人空间请求
 */
@Data
public class PictureSaveToPrivateRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
