package com.nageoffer.shorlink.admin.dto.req;

import lombok.Data;

/**
 * <p>
 * 功能描述: 回收站恢复请求参数
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/11/21
 */
@Data
public class RecycleBinRecoverReqDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

}