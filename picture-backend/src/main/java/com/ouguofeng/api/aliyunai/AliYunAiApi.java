package com.ouguofeng.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.ouguofeng.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.ouguofeng.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.ouguofeng.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.ouguofeng.api.aliyunai.model.CreatePortraitStyleRedrawTaskRequest;
import com.ouguofeng.api.aliyunai.model.CreatePortraitStyleRedrawTaskResponse;
import com.ouguofeng.api.aliyunai.model.GetPortraitStyleRedrawTaskResponse;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.annotation.PreDestroy;

/**
 * 阿里云 AI 接口
 * 接口一，异步创建扩图任务
 * 接口二，轮询查询任务结果
 * 接口三，图像识别标签和分类
 */
@Slf4j
@Component
public class AliYunAiApi {

    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 创建人物风格重绘任务地址
    public static final String CREATE_PORTRAIT_STYLE_REDRAW_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image-generation/generation";

    // AI识别超时时间（秒）
    private static final int AI_RECOGNITION_TIMEOUT = 8;

    // 线程池，用于异步执行AI识别任务
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 应用关闭时清理线程池
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 预定义的图片标签列表（精简版，与PictureController保持一致）
     * AI识别时将从这些标签中选择最合适的
     */
    public static final List<String> PICTURE_TAGS = Arrays.asList(
            // 风格类
            "高清", "创意", "艺术", "简约", "卡通",
            // 场景类
            "风景", "自然", "城市", "建筑", "夜景",
            // 人物类
            "人物", "美女", "儿童", "情侣",
            // 生活类
            "生活", "美食", "旅游", "运动", "宠物",
            // 情感类
            "搞笑", "温馨", "浪漫", "治愈",
            // 主题类
            "科技", "动物", "植物", "节日",
            // 用途类
            "壁纸", "海报", "头像", "背景"
    );

    /**
     * 预定义的图片分类列表（与PictureController保持一致）
     * AI识别时必须从这些分类中选择一个
     */
    public static final List<String> PICTURE_CATEGORIES = Arrays.asList(
            "模板", "电商", "表情包", "素材", "海报", "壁纸", 
            "图标", "插画", "背景图"
    );


    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 必须开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        // 处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            if (createOutPaintingTaskResponse.getCode() != null) {
                String errorMessage = createOutPaintingTaskResponse.getMessage();
                log.error("请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败，" + errorMessage);
            }
            return createOutPaintingTaskResponse;
        }
    }

    /**
     * 查询创建的任务结果
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 ID 不能为空");
        }
        // 处理响应
        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }

    /**
     * 创建人物风格重绘任务
     *
     * @param createPortraitStyleRedrawTaskRequest
     * @return
     */
    public CreatePortraitStyleRedrawTaskResponse createPortraitStyleRedrawTask(CreatePortraitStyleRedrawTaskRequest createPortraitStyleRedrawTaskRequest) {
        if (createPortraitStyleRedrawTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "风格重绘参数为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_PORTRAIT_STYLE_REDRAW_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 必须开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createPortraitStyleRedrawTaskRequest));
        // 处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 风格重绘失败");
            }
            CreatePortraitStyleRedrawTaskResponse createPortraitStyleRedrawTaskResponse = JSONUtil.toBean(httpResponse.body(), CreatePortraitStyleRedrawTaskResponse.class);
            if (createPortraitStyleRedrawTaskResponse.getCode() != null) {
                String errorMessage = createPortraitStyleRedrawTaskResponse.getMessage();
                log.error("请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 风格重绘失败，" + errorMessage);
            }
            return createPortraitStyleRedrawTaskResponse;
        }
    }

    /**
     * 查询人物风格重绘任务结果
     *
     * @param taskId
     * @return
     */
    public GetPortraitStyleRedrawTaskResponse getPortraitStyleRedrawTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 ID 不能为空");
        }
        // 处理响应
        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetPortraitStyleRedrawTaskResponse.class);
        }
    }

    /* 核心方法：调用Qwen-Image模型生成图像
     * @throws ApiException SDK调用异常（如API接口错误、参数非法等）
     * @throws NoApiKeyException API密钥缺失异常（未配置或配置错误）
     * @throws UploadFileException 文件上传异常（当前示例未涉及文件上传，预留异常）
     * @throws IOException JSON序列化/反序列化异常（如结果转换失败）
     */
    public String AiCreatePicture(String text)
            throws ApiException, NoApiKeyException, UploadFileException, IOException {

        System.out.println("提示词: " + text);

        // 1. 创建多模态对话客户端实例（阿里云SDK提供的工具类，用于发起生图请求）
        MultiModalConversation conv = new MultiModalConversation();

        // 2. 构建用户消息（生图的核心输入：角色+提示词）
        MultiModalMessage userMessage = MultiModalMessage.builder() // 使用建造者模式（Builder）创建消息对象，代码更简洁
                .role(Role.USER.getValue()) // 设置消息角色为"user"（API要求：生图请求必须由用户角色发起）
                .content( // 设置消息内容：List格式，API要求仅含1个text类型的元素
                        Arrays.asList( // 转为List（因content参数要求为集合类型）
                                // 单个元素为Map：key固定为"text"，value为正向提示词（描述生成图像的细节）
                                Collections.singletonMap(
                                        "text", text

                                        //因为方便，这里我直接用的String text来传参，如果用createImageTaskRequest可以参考下面                                  //createImageTaskRequest.getInput().getMessages().get(0).getContent().get(0).getText()
                                        //例如："一副典雅庄重的对联悬挂于厅堂之中，房间是个安静古典的中式布置，桌子上放着一些青花瓷，对联上左书“义本生知人机同道善思新”，右书“通云赋智乾坤启数高志远”， 横批“智启通义”，字体飘逸，中间挂在一着一副中国风的画作，内容是岳阳楼。"

                                )
                        )
                ).build(); // 完成消息对象构建

        // 3. 构建生图参数（可选参数，控制图像生成规则）
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("watermark", false); // 开启水印：生成的图像右下角添加“Qwen-Image生成”水印
        parameters.put("prompt_extend", true); // 开启提示词智能改写：SDK会优化输入的prompt，提升生图效果（耗时增加3-4秒）
        parameters.put("negative_prompt", ""); // 反向提示词：空字符串表示不限制“不希望出现的内容”
        parameters.put("size", "1328*1328"); // 图像分辨率：默认值，宽1328像素、高1328像素（1:1比例）

        // 4. 构建完整的生图请求参数（整合API密钥、模型名、消息、参数）
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey) // 传入阿里云API密钥（身份认证核心，缺失会抛NoApiKeyException）
                .model("qwen-image-plus") // 指定调用的模型：固定为"qwen-image"（通义千问生图模型）
                .messages(Collections.singletonList(userMessage)) // 传入用户消息列表（API要求仅支持单轮对话，故用 singletonList 生成单元素列表）
                .parameters(parameters) // 传入步骤3构建的生图参数
                .build(); // 完成请求参数构建

        // 5. 发起生图请求并获取结果
        MultiModalConversationResult result = conv.call(param); // 调用SDK的call方法，发送请求到阿里云API
        ThrowUtils.throwIf(result.getOutput().getChoices().isEmpty(), ErrorCode.PARAMS_ERROR, "Choices为空");
        ThrowUtils.throwIf(result.getOutput().getChoices().get(0).getMessage().getContent().isEmpty(), ErrorCode.PARAMS_ERROR, "Content为空");

        Map<String, Object> imageUrlMap = result.getOutput().getChoices().get(0).getMessage().getContent().get(0);

        return (String) imageUrlMap.get("image");
        //还是因为图方便，直接返回Url了

    }

    /**
     * AI 识别图片标签和分类（带超时控制）
     * 使用通义千问视觉理解模型识别图片内容，自动生成标签和分类
     * 超时时间：8秒，超时后返回默认值
     * 
     * @param imageUrl 图片URL地址
     * @return ImageRecognitionResult 包含标签列表和分类的识别结果
     */
    public ImageRecognitionResult recognizeImageTagsAndCategory(String imageUrl) {
        
        if (StrUtil.isBlank(imageUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片URL不能为空");
        }
        
        log.info("开始AI识别图片标签和分类，图片URL: {}", imageUrl);
        long startTime = System.currentTimeMillis();
        
        // 使用Future实现超时控制
        Future<ImageRecognitionResult> future = executorService.submit(() -> {
            return doRecognizeImage(imageUrl);
        });
        
        try {
            // 等待结果，最多等待指定秒数
            ImageRecognitionResult result = future.get(AI_RECOGNITION_TIMEOUT, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            log.info("AI识别成功，耗时: {}ms", duration);
            return result;
        } catch (TimeoutException e) {
            // 超时，取消任务
            future.cancel(true);
            long duration = System.currentTimeMillis() - startTime;
            log.warn("AI识别超时（超过{}秒），使用默认值，实际耗时: {}ms", AI_RECOGNITION_TIMEOUT, duration);
            return getDefaultRecognitionResult();
        } catch (Exception e) {
            // 其他异常
            long duration = System.currentTimeMillis() - startTime;
            log.error("AI识别失败，使用默认值，耗时: {}ms", duration, e);
            return getDefaultRecognitionResult();
        }
    }
    
    /**
     * 执行实际的图片识别（内部方法）
     * @param imageUrl 图片URL
     * @return 识别结果
     */
    private ImageRecognitionResult doRecognizeImage(String imageUrl) {
        try {
            // 1. 创建多模态对话客户端
            MultiModalConversation conv = new MultiModalConversation();
            
            // 2. 构建包含图片和提示词的用户消息
            // 创建图片内容
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("image", imageUrl);
            
            // 创建文本提示词内容
            Map<String, Object> textContent = new HashMap<>();
            
            // 构建标签选项字符串
            String tagOptions = String.join("、", PICTURE_TAGS);
            // 构建分类选项字符串
            String categoryOptions = String.join("、", PICTURE_CATEGORIES);
            
            // 精简提示词，提高响应速度
            String prompt = "分析图片，从以下选项中选择：\n" +
                    "标签（选3个）：" + tagOptions + "\n" +
                    "分类（选1个）：" + categoryOptions + "\n" +
                    "直接返回JSON格式：{\"tags\": [\"标签1\", \"标签2\", \"标签3\"], \"category\": \"分类\"}";
            textContent.put("text", prompt);
            
            // 构建消息内容列表（先图片后文本）
            List<Map<String, Object>> contentList = new ArrayList<>();
            contentList.add(imageContent);
            contentList.add(textContent);
            
            // 构建用户消息
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(contentList)
                    .build();
            
            // 3. 构建请求参数
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model("qwen-vl-plus")  // 使用更快速的视觉理解模型（qwen-vl-plus比qwen-vl-max更快）
                    .messages(Collections.singletonList(userMessage))
                    .build();
            
            // 4. 调用API获取识别结果
            MultiModalConversationResult result = conv.call(param);
            
            // 5. 解析响应结果
            if (result.getOutput() == null || 
                result.getOutput().getChoices() == null || 
                result.getOutput().getChoices().isEmpty()) {
                log.error("AI识别返回结果为空");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI识别失败");
            }
            
            // 获取AI返回的文本内容
            String responseText = result.getOutput().getChoices().get(0)
                    .getMessage().getContent().get(0).get("text").toString();
            
            log.info("AI识别原始返回结果: {}", responseText);
            
            // 6. 解析JSON格式的识别结果
            return parseRecognitionResult(responseText);
            
        } catch (Exception e) {
            log.error("AI识别图片失败", e);
            // 如果AI识别失败，返回默认值
            return getDefaultRecognitionResult();
        }
    }
    
    /**
     * 解析AI识别结果
     * @param responseText AI返回的文本
     * @return 解析后的识别结果
     */
    private ImageRecognitionResult parseRecognitionResult(String responseText) {
        try {
            // 尝试提取JSON部分（有时AI会返回额外的说明文字）
            String jsonStr = responseText;
            int startIndex = responseText.indexOf("{");
            int endIndex = responseText.lastIndexOf("}");
            if (startIndex >= 0 && endIndex > startIndex) {
                jsonStr = responseText.substring(startIndex, endIndex + 1);
            }
            
            // 解析JSON
            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(jsonStr);
            
            ImageRecognitionResult result = new ImageRecognitionResult();
            
            // 提取标签列表
            if (jsonObject.containsKey("tags")) {
                cn.hutool.json.JSONArray tagsArray = jsonObject.getJSONArray("tags");
                List<String> tags = new ArrayList<>();
                for (Object tag : tagsArray) {
                    tags.add(tag.toString());
                }
                result.setTags(tags);
            }
            
            // 提取分类
            if (jsonObject.containsKey("category")) {
                result.setCategory(jsonObject.getStr("category"));
            }
            
            log.info("解析识别结果成功 - 标签: {}, 分类: {}", result.getTags(), result.getCategory());
            return result;
            
        } catch (Exception e) {
            log.error("解析AI识别结果失败，返回默认值", e);
            return getDefaultRecognitionResult();
        }
    }
    
    /**
     * 获取默认的识别结果（当AI识别失败时使用）
     * 返回一些通用的标签和默认分类
     */
    private ImageRecognitionResult getDefaultRecognitionResult() {
        ImageRecognitionResult result = new ImageRecognitionResult();
        // 使用预定义标签中的通用标签
        result.setTags(Arrays.asList("高清", "创意", "素材"));
        // 使用预定义分类中的默认分类
        result.setCategory("素材");
        log.info("使用默认识别结果 - 标签: {}, 分类: {}", result.getTags(), result.getCategory());
        return result;
    }
    
    /**
     * 图像识别结果内部类
     */
    public static class ImageRecognitionResult {
        private List<String> tags;
        private String category;
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
    }

}
