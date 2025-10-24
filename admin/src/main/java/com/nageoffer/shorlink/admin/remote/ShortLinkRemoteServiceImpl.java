package com.nageoffer.shorlink.admin.remote;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.nageoffer.shorlink.admin.common.biz.user.UserContext;
import com.nageoffer.shorlink.admin.common.convention.result.Result;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkGroupCountReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shorlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shorlink.admin.remote.dto.resp.ShortLinkGroupCountRespDTO;
import com.nageoffer.shorlink.admin.remote.dto.resp.ShortLinkPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 功能描述: 短链接中台远程调用服务实现类
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkRemoteServiceImpl implements ShortLinkRemoteService {
    
    private final RestTemplate restTemplate;
    
    @Value("${short-link.project.url:http://127.0.0.1:8001}")
    private String projectServiceUrl;
    
    @Override
    public Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam) {
        String url = projectServiceUrl + "/api/short-link/v1/create";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<ShortLinkCreateReqDTO> entity = new HttpEntity<>(requestParam, headers);
        
        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            return JSON.parseObject(response, new TypeReference<Result<ShortLinkCreateRespDTO>>() {});
        } catch (Exception e) {
            log.error("远程调用创建短链接失败", e);
            throw new RuntimeException("远程调用创建短链接失败: " + e.getMessage());
        }
    }

    /**
     * 修改短链接
     */
    @Override
    public Result<Void> updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        String url = projectServiceUrl + "/api/short-link/v1/update";
        
        log.info("远程调用修改短链接开始 - URL: {}", url);
        log.info("远程调用修改短链接 - 请求参数: {}", JSON.toJSONString(requestParam));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 传递用户信息到project服务（关键修复！）
        String username = UserContext.getUsername();
        if (username != null) {
            headers.set("username", username);
            log.info("传递用户信息 - username: {}", username);
        }

        HttpEntity<ShortLinkUpdateReqDTO> entity = new HttpEntity<>(requestParam, headers);

        try {
            // 使用 PUT 方法
            restTemplate.put(url, entity);
            log.info("远程调用修改短链接成功");
            return new Result<Void>().setCode("0").setMessage("修改成功");
        } catch (Exception e) {
            log.error("远程调用修改短链接失败", e);
            throw new RuntimeException("远程调用修改短链接失败: " + e.getMessage());
        }
    }

    @Override
    public Result<ShortLinkPageResult> pageShortLink(ShortLinkPageReqDTO requestParam) {
        String url = projectServiceUrl + "/api/short-link/v1/page?gid={gid}&current={current}&size={size}";
        
        Map<String, Object> params = new HashMap<>();
        params.put("gid", requestParam.getGid());
        params.put("current", requestParam.getCurrent());
        params.put("size", requestParam.getSize());
        
        // 添加日志：打印请求信息
        log.info("远程调用分页查询 - URL: {}", url);
        log.info("远程调用分页查询 - 参数: gid={}, current={}, size={}", 
                requestParam.getGid(), requestParam.getCurrent(), requestParam.getSize());
        
        try {
            String response = restTemplate.getForObject(url, String.class, params);
            
            // 添加日志：打印响应
            log.info("远程调用分页查询 - 响应: {}", response);
            
            // 解析结果 - 使用简单的 ShortLinkPageResult 替代 IPage
            Result<ShortLinkPageResult> result = JSON.parseObject(response, new TypeReference<Result<ShortLinkPageResult>>() {});
            
            // 添加日志：打印解析后的结果
            log.info("远程调用分页查询 - 解析后的result: {}", result);
            log.info("远程调用分页查询 - 解析后的data: {}", result.getData());
            
            return result;
        } catch (Exception e) {
            log.error("远程调用分页查询短链接失败", e);
            throw new RuntimeException("远程调用分页查询短链接失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<ShortLinkGroupCountRespDTO>> countByGidList(ShortLinkGroupCountReqDTO requestParam) {
        String url = projectServiceUrl + "/api/short-link/v1/count";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<ShortLinkGroupCountReqDTO> entity = new HttpEntity<>(requestParam, headers);
        
        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            return JSON.parseObject(response, new TypeReference<Result<List<ShortLinkGroupCountRespDTO>>>() {});
        } catch (Exception e) {
            log.error("远程调用批量查询分组短链接数量失败", e);
            throw new RuntimeException("远程调用批量查询分组短链接数量失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> getTitleByUrl(String url) {
        String apiUrl = projectServiceUrl + "/api/short-link/v1/title?url=" + url;
        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            return JSON.parseObject(response, new TypeReference<Result<String>>(){});
        } catch (Exception ex) {
            log.error("远程调用获取网站标题失败", ex);
            throw new RuntimeException("远程调用获取网站标题失败" + ex.getMessage());
        }
    }
} 