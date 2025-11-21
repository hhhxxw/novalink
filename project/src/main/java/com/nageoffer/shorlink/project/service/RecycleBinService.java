package com.nageoffer.shorlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shorlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shorlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shorlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shorlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shorlink.project.dto.resp.ShortLinkPageRespDTO;

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

    /**
     * 分页查询短链接
     * @param requestParam 分页查询短链接请求参数
     * @return 短链接分页返回结果
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * 恢复短链接
     * @param requestParam 请求参数： gid， fullShortUrl
     */
    void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam);
}
