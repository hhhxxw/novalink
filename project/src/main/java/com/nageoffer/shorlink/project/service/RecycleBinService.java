package com.nageoffer.shorlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shorlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shorlink.project.dto.req.RecycleBinSaveReqDTO;

/**
 * <p>
 * 功能描述: 回收站管理接口
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/31
 */
public interface RecycleBinService extends IService<ShortLinkDO> {
    /**
     * 保存回收站
     * @param requestParam 请求参数
     */
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);
}
