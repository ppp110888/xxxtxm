package com.xxxtxm.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限认证配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Value("${codemate.security.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    /** 注册 Sa-Token 拦截器 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 除登录/注册外，所有 /api/** 需要认证
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register"
                );
    }

    /** 自定义角色与权限加载 */
    @Bean
    public StpInterface stpInterface() {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                // 权限码列表，后续可从数据库加载
                return Collections.emptyList();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                // 角色列表，从 Sa-Token Session 中获取
                String role = (String) StpUtil.getSessionByLoginId(loginId)
                        .get("role");
                return role != null ? Collections.singletonList(role) : Collections.emptyList();
            }
        };
    }

    /** CORS 跨域配置 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
