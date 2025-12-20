package com.ouguofeng.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户关注请求
 */
@Data
public class UserFollowRequest implements Serializable {

    /**
     * 被关注者 id
     */
    private Long followeeId;

    private static final long serialVersionUID = 1L;
}






