package com.ouguofeng.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除请求
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * 根据id执行删除操作
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}