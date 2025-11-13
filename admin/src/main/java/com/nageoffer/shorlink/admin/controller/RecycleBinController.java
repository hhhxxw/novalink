package com.nageoffer.shorlink.admin.controller;

import com.nageoffer.shorlink.admin.common.convention.result.Result;
import com.nageoffer.shorlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shorlink.admin.remote.ShortLinkRemoteService;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.resp.ShortLinkPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("回收站分页查询 - 请求参数: gid={}, current={}, size={}", 
                requestParam.getGid(), requestParam.getCurrent(), requestParam.getSize());
        
        // 参数验证
        if (requestParam.getGid() == null || requestParam.getGid().trim().isEmpty()) {
            log.warn("回收站分页查询 - gid 参数为空，请检查前端是否正确传入分组标识");
            return new Result<ShortLinkPageResult>()
                    .setCode("A000400")
                    .setMessage("分组标识不能为空，请先选择一个分组");
        }
        
        if (requestParam.getCurrent() == null || requestParam.getCurrent() <= 0) {
            requestParam.setCurrent(1L);
        }
        
        if (requestParam.getSize() == null || requestParam.getSize() <= 0) {
            requestParam.setSize(10L);
        }
        
        log.info("回收站分页查询 - 验证后参数: gid={}, current={}, size={}", 
                requestParam.getGid(), requestParam.getCurrent(), requestParam.getSize());
        
        return shortLinkRemoteService.pageRecycleBinShortLink(requestParam);
    }
}
