package com.nageoffer.shorlink.project.controller;

import com.nageoffer.shorlink.project.common.convention.result.Result;
import com.nageoffer.shorlink.project.common.convention.result.Results;
import com.nageoffer.shorlink.project.service.GetTitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 功能描述: URL标题控制层
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/23
 */
@Tag(name = "URL标题管理", description = "URL标题相关接口")
@RestController
@RequiredArgsConstructor
public class UrlTitleController {
    private final GetTitleService getTitleService;

    /**
     * 根据url获取对应网站的标题
     * @param url url
     * @return url网站对应的标题
     */
    @Operation(summary = "获取网站标题", description = "根据URL获取对应网站的标题信息")
    @GetMapping("/api/short-link/v1/title")
    public Result<String> getTitleByUrl(
            @Parameter(description = "网站URL地址", required = true, example = "https://www.baidu.com")
            @RequestParam("url") String url) {
        return Results.success(getTitleService.getTitleByUrl(url));
    }
}
