package com.nageoffer.shorlink.project.toolkit;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <p>
 * 功能描述: 网站图标（Favicon）获取工具类
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/28
 */
@Slf4j
public class FaviconUtil {

    // 3秒超时
    private static final int TIMEOUT = 3000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * 获取网站favicon的URL
     * <p>
     * 实现策略：
     * 1. 优先从HTML解析获取真实favicon路径
     * 2. 尝试默认路径 /favicon.ico
     * 3. 降级使用第三方服务（Google Favicon API）
     * </p>
     *
     * @param originUrl 原始网站URL
     * @return favicon的完整URL，获取失败返回默认图标URL
     */
    public static String getFaviconUrl(String originUrl) {
        if (originUrl == null || originUrl.trim().isEmpty()) {
            return getDefaultFavicon();
        }

        try {
            // 1. 解析URL获取基础信息
            URL url = new URL(originUrl);
            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();
            
            String baseUrl = protocol + "://" + host;
            if (port != -1 && port != 80 && port != 443) {
                baseUrl += ":" + port;
            }

            // 2. 尝试从HTML中解析favicon
            String faviconFromHtml = extractFaviconFromHtml(originUrl, baseUrl);
            if (faviconFromHtml != null) {
                log.debug("从HTML解析获取favicon: {}", faviconFromHtml);
                return faviconFromHtml;
            }

            // 3. 尝试默认路径 /favicon.ico
            String defaultFaviconPath = baseUrl + "/favicon.ico";
            if (checkUrlExists(defaultFaviconPath)) {
                log.debug("使用默认路径favicon: {}", defaultFaviconPath);
                return defaultFaviconPath;
            }

            // 4. 降级使用第三方服务
            log.debug("使用第三方服务获取favicon: {}", host);
            return getThirdPartyFavicon(host);

        } catch (Exception e) {
            log.warn("获取favicon失败，使用默认图标。URL: {}, 错误: {}", originUrl, e.getMessage());
            return getDefaultFavicon();
        }
    }

    /**
     * 从HTML中提取favicon链接
     *
     * @param originUrl 原始URL
     * @param baseUrl 基础URL
     * @return favicon URL，失败返回null
     */
    private static String extractFaviconFromHtml(String originUrl, String baseUrl) {
        try {
            Document doc = Jsoup.connect(originUrl)
                    .timeout(TIMEOUT)
                    .userAgent(USER_AGENT)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .get();

            // 查找各种可能的favicon link标签
            // rel属性可能是: icon, shortcut icon, apple-touch-icon 等
            Element iconLink = doc.selectFirst("link[rel~=(?i)(icon|shortcut icon)]");
            
            if (iconLink != null) {
                String href = iconLink.attr("href");
                if (href != null && !href.isEmpty()) {
                    return resolveUrl(baseUrl, href);
                }
            }

            // 尝试查找apple-touch-icon作为备选
            Element appleIcon = doc.selectFirst("link[rel~=(?i)apple-touch-icon]");
            if (appleIcon != null) {
                String href = appleIcon.attr("href");
                if (href != null && !href.isEmpty()) {
                    return resolveUrl(baseUrl, href);
                }
            }

        } catch (Exception e) {
            log.debug("解析HTML获取favicon失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 解析相对URL为绝对URL
     *
     * @param baseUrl 基础URL
     * @param href 链接地址
     * @return 绝对URL
     */
    private static String resolveUrl(String baseUrl, String href) {
        try {
            if (href.startsWith("http://") || href.startsWith("https://")) {
                // 已经是绝对路径
                return href;
            } else if (href.startsWith("//")) {
                // 协议相对路径
                return "https:" + href;
            } else if (href.startsWith("/")) {
                // 根路径
                return baseUrl + href;
            } else {
                // 相对路径
                return baseUrl + "/" + href;
            }
        } catch (Exception e) {
            log.debug("解析URL失败: baseUrl={}, href={}", baseUrl, href);
            return baseUrl + "/favicon.ico";
        }
    }

    /**
     * 检查URL是否可访问
     *
     * @param urlStr URL字符串
     * @return true=可访问，false=不可访问
     */
    private static boolean checkUrlExists(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            
            int responseCode = conn.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
            
        } catch (Exception e) {
            log.debug("检查URL可用性失败: {}", urlStr);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 使用第三方服务获取favicon
     * <p>
     * 备选服务：
     * 1. Google: https://www.google.com/s2/favicons?domain={domain}&sz=64
     * 2. DuckDuckGo: https://icons.duckduckgo.com/ip3/{domain}.ico
     * 3. Favicon Kit: https://api.faviconkit.com/{domain}/64
     * </p>
     *
     * @param domain 域名
     * @return 第三方服务的favicon URL
     */
    private static String getThirdPartyFavicon(String domain) {
        // 优先使用Google服务（国内可能需要切换为其他服务）
        return "https://www.google.com/s2/favicons?domain=" + domain + "&sz=64";
        
        // 备选方案（如果Google不可用，可以切换为以下服务）:
        // return "https://icons.duckduckgo.com/ip3/" + domain + ".ico";
        // return "https://api.faviconkit.com/" + domain + "/64";
    }

    /**
     * 返回默认图标URL
     *
     * @return 默认图标URL
     */
    private static String getDefaultFavicon() {
        // 返回一个通用的默认图标
        return "https://www.google.com/s2/favicons?domain=default&sz=64";
    }
}

