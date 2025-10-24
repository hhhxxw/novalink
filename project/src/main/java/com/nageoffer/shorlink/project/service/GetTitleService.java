package com.nageoffer.shorlink.project.service;

/**
 * <p>
 * 功能描述: URL接口层
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/23
 */
public interface GetTitleService {
    
    /**
     * 根据URL获取网站标题
     * @param url 目标网站URL
     * @return 网站标题，获取失败返回null或默认值
     */
    String getTitleByUrl(String url);
}
