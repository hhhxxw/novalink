package com.nageoffer.shorlink.admin.remote;

import com.nageoffer.shorlink.admin.common.convention.result.Result;
import com.nageoffer.shorlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkGroupCountReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shorlink.admin.remote.dto.resp.ShortLinkGroupCountRespDTO;
import com.nageoffer.shorlink.admin.remote.dto.resp.ShortLinkPageResult;

import java.util.List;

/**
 * <p>
 * 功能描述: 短链接中台远程调用服务
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/9/30
 */
public interface ShortLinkRemoteService {
    
    /**
     * 创建短链接
     */
    Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     * 修改短链接
     */
    Result<Void> updateShortLink(ShortLinkUpdateReqDTO requestParam);
    /**
     * 分页查询短链接
     */
    Result<ShortLinkPageResult> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * 批量查询分组短链接数量
     */
    Result<List<ShortLinkGroupCountRespDTO>> countByGidList(ShortLinkGroupCountReqDTO requestParam);


    /**
     * 根据url获取网站标题
     */
    Result<String> getTitleByUrl(String url);

    /**
     * 保存回收站
     */
    Result<Void> saveRecycleBin(RecycleBinSaveReqDTO requestParam);
}
