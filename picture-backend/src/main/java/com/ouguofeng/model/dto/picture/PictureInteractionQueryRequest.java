package com.ouguofeng.model.dto.picture;

import com.ouguofeng.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 图片交互查询请求（查询用户点赞/收藏的图片）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PictureInteractionQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 id（可选，如果不传则查询当前登录用户）
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}

