package com.nageoffer.shorlink.project.controller;

import com.nageoffer.shorlink.project.common.convention.result.Result;
import com.nageoffer.shorlink.project.common.convention.result.Results;
import com.nageoffer.shorlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shorlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
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

    private final RecycleBinService recycleBinService;

    @PostMapping("/api/short-link/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
        recycleBinService.saveRecycleBin(requestParam);
        return Results.success();
    }
}
