package com.nageoffer.shorlink.project.service.impl;

import cn.hutool.core.util.StrUtil;
import com.nageoffer.shorlink.project.service.GetTitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 功能描述: URL标题接口层实现
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetTitleServiceImpl implements GetTitleService {

    /**
     * 连接超时时间（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 5000;
    
    /**
     * 读取超时时间（毫秒）
     */
    private static final int READ_TIMEOUT = 5000;
    
    /**
     * 默认标题（获取失败时返回）
     */
    private static final String DEFAULT_TITLE = "未知网站";

    @Override
    public String getTitleByUrl(String url) {
        // 1. 参数验证
        if (StrUtil.isBlank(url)) {
            log.warn("获取网站标题失败：URL为空");
            return DEFAULT_TITLE;
        }
        
        // 2. URL格式校验：确保以http或https开头
        String normalizedUrl = normalizeUrl(url);
        
        try {
            // 3. 使用Jsoup连接并获取HTML文档
            Document document = Jsoup.connect(normalizedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(CONNECT_TIMEOUT)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();
            
            // 4. 提取title标签内容
            String title = document.title();
            
            // 5. 判断标题是否为空
            if (StrUtil.isNotBlank(title)) {
                log.info("成功获取网站标题：URL={}, title={}", normalizedUrl, title);
                return title.trim();
            } else {
                log.warn("网站标题为空：URL={}", normalizedUrl);
                return DEFAULT_TITLE;
            }
            
        } catch (Exception e) {
            // 6. 异常处理：网络超时、连接失败等
            log.error("获取网站标题失败：URL={}, 错误信息={}", normalizedUrl, e.getMessage());
            return DEFAULT_TITLE;
        }
    }
    
    /**
     * 标准化URL格式
     * @param url 原始URL
     * @return 标准化后的URL
     */
    private String normalizeUrl(String url) {
        String trimmedUrl = url.trim();
        // 如果没有协议，默认添加https
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return "https://" + trimmedUrl;
        }
        return trimmedUrl;
    }
}
