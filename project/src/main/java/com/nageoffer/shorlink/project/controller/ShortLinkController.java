package com.nageoffer.shorlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shorlink.project.common.convention.result.Result;
import com.nageoffer.shorlink.project.common.convention.result.Results;
import com.nageoffer.shorlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shorlink.project.dto.req.ShortLinkGroupCountReqDTO;
import com.nageoffer.shorlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shorlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shorlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shorlink.project.dto.resp.ShortLinkGroupCountRespDTO;
import com.nageoffer.shorlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shorlink.project.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 功能描述: 短链接控制层
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/9/19
 */
@Tag(name = "短链接管理", description = "短链接相关接口")
@RestController
@RequiredArgsConstructor
public class ShortLinkController {
    private final ShortLinkService shortLinkService;

    /**
     * 短链接跳转
     * @param shortUri 短链接后缀
     * @param request Http 请求
     * @param response Http 响应
     */
    @Operation(summary = "短链接跳转", description = "根据短链接后缀跳转到原始URL")
    @GetMapping("/{short-uri}")
    public void restoreUrl(
            @Parameter(description = "短链接后缀", required = true, example = "abc123")
            @PathVariable("short-uri") String shortUri, 
            ServletRequest request, 
            ServletResponse response) throws IOException {
        // 排除swagger等系统路径，避免被短链接拦截
        if (shortUri.startsWith("swagger") || shortUri.startsWith("v3") || 
            shortUri.startsWith("api") || shortUri.equals("favicon.ico")) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        shortLinkService.restoreUrl(shortUri, (HttpServletRequest) request, (HttpServletResponse) response);
    }
    /**
     * 创建短链接
     */
    @Operation(summary = "创建短链接", description = "根据原始URL创建短链接")
    @PostMapping("/api/short-link/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return Results.success(shortLinkService.createShortLink(requestParam));
    }

    /**
     * 修改短链接
     */
    @Operation(summary = "修改短链接", description = "修改短链接信息")
    @PutMapping("/api/short-link/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam){
        shortLinkService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
     * 分页查询短链接
     */
    @Operation(summary = "分页查询短链接", description = "分页查询短链接列表")
    @GetMapping("/api/short-link/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return Results.success(shortLinkService.pageShortLink(requestParam));
    }

    /**
     * 统计短链接数量
     */
    @Operation(summary = "统计短链接数量", description = "根据分组ID列表统计短链接数量")
    @PostMapping("/api/short-link/v1/count")
    public Result<List<ShortLinkGroupCountRespDTO>> countByGidList(@RequestBody ShortLinkGroupCountReqDTO requestParam) {
        return Results.success(shortLinkService.countByGidList(requestParam.getGidList()));
    }
}
