package com.nageoffer.shorlink.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>
 * 功能描述: Web MVC配置
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/24
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    // springdoc-openapi-starter-webmvc-ui 已内置资源处理，无需额外配置
    // 只需要在ShortLinkController中排除swagger路径即可
}