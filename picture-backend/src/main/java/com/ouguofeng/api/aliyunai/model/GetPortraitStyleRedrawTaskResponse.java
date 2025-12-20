package com.ouguofeng.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询人物风格重绘任务响应类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetPortraitStyleRedrawTaskResponse {

    /**
     * 请求唯一标识
     */
    private String requestId;

    /**
     * 输出信息
     */
    private Output output;

    /**
     * 使用统计
     */
    private Usage usage;

    /**
     * 表示任务的输出信息
     */
    @Data
    public static class Output {

        /**
         * 任务 ID
         */
        private String taskId;

        /**
         * 任务状态
         * <ul>
         *     <li>PENDING：排队中</li>
         *     <li>RUNNING：处理中</li>
         *     <li>SUCCEEDED：执行成功</li>
         *     <li>FAILED：执行失败</li>
         *     <li>CANCELED：任务已取消</li>
         *     <li>UNKNOWN：任务不存在或状态未知</li>
         * </ul>
         */
        private String taskStatus;

        /**
         * 提交时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String submitTime;

        /**
         * 调度时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String scheduledTime;

        /**
         * 开始时间
         */
        private String startTime;

        /**
         * 结束时间
         * 格式：YYYY-MM-DD HH:mm:ss
         */
        private String endTime;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 错误码
         */
        private Integer errorCode;

        /**
         * 风格索引值
         */
        private Integer styleIndex;

        /**
         * 任务结果列表
         */
        private List<Result> results;

        /**
         * 任务指标信息
         */
        private TaskMetrics taskMetrics;
    }

    /**
     * 任务结果
     */
    @Data
    public static class Result {
        /**
         * 输出图像的 URL
         */
        private String url;
    }

    /**
     * 表示任务的统计信息
     */
    @Data
    public static class TaskMetrics {

        /**
         * 总任务数
         */
        private Integer TOTAL;

        /**
         * 成功任务数
         */
        private Integer SUCCEEDED;

        /**
         * 失败任务数
         */
        private Integer FAILED;
    }

    /**
     * 使用统计
     */
    @Data
    public static class Usage {
        /**
         * 成功生成的图片数量
         */
        private Integer imageCount;
    }
}

