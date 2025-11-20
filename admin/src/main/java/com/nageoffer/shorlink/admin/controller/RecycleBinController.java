package com.nageoffer.shorlink.admin.controller;

import com.nageoffer.shorlink.admin.common.convention.result.Result;
import com.nageoffer.shorlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shorlink.admin.remote.ShortLinkRemoteService;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.resp.ShortLinkPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 功能描述: 回收站控制层
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/31
 */
@RestController
@RequiredArgsConstructor
public class RecycleBinController {

    private final ShortLinkRemoteService shortLinkRemoteService;

    /**
     * 保存回收站
     * @param requestParam 请求参数
     * @return 返回
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
        return shortLinkRemoteService.saveRecycleBin(requestParam);
    }

    /**
     * 分页查询回收站短链接
     */
    @GetMapping("/api/short-link/admin/v1/recycle-bin/page")
    public Result<ShortLinkPageResult> pageRecycleBinShortLink(ShortLinkPageReqDTO requestParam) {
        return shortLinkRemoteService.pageRecycleBinShortLink(requestParam);
    }
}
