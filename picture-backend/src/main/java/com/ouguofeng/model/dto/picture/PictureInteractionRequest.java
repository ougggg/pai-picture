package com.ouguofeng.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片交互请求（点赞/收藏）
 */
@Data
public class PictureInteractionRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}

