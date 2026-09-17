package com.xxxtxm.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 大模型配置属性
 *
 * <p>绑定 application.yml 中 codemate.ai 配置段，
 * 支持环境变量 AI_API_KEY 覆盖。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "codemate.ai")
public class AiProperties {

    /** API 密钥（通过环境变量注入，不写入配置文件） */
    private String apiKey = "";

    /** API 基础地址 */
    private String apiUrl = "https://api.deepseek.com/v1";

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 默认温度（0-2，越高越随机） */
    private double temperature = 0.7;

    /** 最大 Token 数 */
    private int maxTokens = 2048;

    /** 是否启用 AI 功能（未配置 API Key 时自动降级） */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
