package com.nageoffer.shorlink.admin.dto.req;

import lombok.Data;

/**
 * <p>
 * 功能描述: 回收站保存请求参数
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/31
 */
@Data
public class RecycleBinSaveReqDTO {

    /**
     * 分组标识（目标分组）
     */
    private String gid;

    /**u
     * 完整短链接
     */
    private String fullShortUrl;

}