package com.nageoffer.shorlink.project.dto.req;

import lombok.Data;

/**
 * <p>
 * 功能描述: 回收站删除请求参数
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/11/24
 */
@Data
public class RecycleBinRemoveReqDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

}