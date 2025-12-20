package com.ouguofeng.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建人物风格重绘任务请求
 */
@Data
public class CreatePortraitStyleRedrawTaskRequest implements Serializable {

    /**
     * 模型名称，必选
     * 示例值：wanx-style-repaint-v1
     */
    private String model = "wanx-style-repaint-v1";

    /**
     * 输入图像信息，必选
     */
    private Input input;

    @Data
    public static class Input {
        /**
         * 必选，输入的图像URL地址
         * 支持公网可访问的HTTP/HTTPS地址，不包含中文字符
         * 支持传入Base64编码字符串
         */
        @Alias("image_url")
        private String imageUrl;

        /**
         * 必选，选择一个预置的风格索引值
         * -1：使用参考图像风格（需提供style_ref_url）
         * 0：复古漫画
         * 1：3D童话
         * 2：二次元
         * 3：小清新
         * 4：未来科技
         * 5：国画古风
         * 6：将军百战
         * 7：炫彩卡通
         * 8：清雅国风
         * 9：喜迎新年
         * 14：国风工笔
         * 15：恭贺新禧
         * 30：童话世界
         * 31：黏土世界
         * 32：像素世界
         * 33：冒险世界
         * 34：日漫世界
         * 35：3D世界
         * 36：二次元世界
         * 37：手绘世界
         * 38：蜡笔世界
         * 39：冰箱贴世界
         * 40：吧唧世界
         */
        @Alias("style_index")
        private Integer styleIndex;

        /**
         * 可选，当style_index=-1时，必须传入，其他风格无需传入
         * 风格参考图像URL地址
         * 支持公网可访问的HTTP/HTTPS地址，不包含中文字符
         * 支持传入Base64编码字符串
         */
        @Alias("style_ref_url")
        private String styleRefUrl;
    }
}

