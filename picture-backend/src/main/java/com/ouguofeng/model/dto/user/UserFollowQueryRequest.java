package com.ouguofeng.model.dto.user;

import com.ouguofeng.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户关注查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserFollowQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 id（可选，如果不传则查询当前登录用户）
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}






