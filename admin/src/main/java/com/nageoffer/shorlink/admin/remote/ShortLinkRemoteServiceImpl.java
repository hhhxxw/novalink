package com.nageoffer.shorlink.admin.remote;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.nageoffer.shorlink.admin.common.biz.user.UserContext;
import com.nageoffer.shorlink.admin.common.convention.result.Result;
import com.nageoffer.shorlink.admin.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shorlink.admin.dto.req.RecycleBinSaveReqDTO;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

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
        // 使用 UriComponentsBuilder 正确构建 URL，只添加非 null 参数
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(projectServiceUrl + "/api/short-link/v1/page");
        
        if (requestParam.getGid() != null) {
            uriBuilder.queryParam("gid", requestParam.getGid());
        }
        if (requestParam.getCurrent() != null) {
            uriBuilder.queryParam("current", requestParam.getCurrent());
        }
        if (requestParam.getSize() != null) {
            uriBuilder.queryParam("size", requestParam.getSize());
        }
        
        String url = uriBuilder.toUriString();
        
        // 添加日志：打印请求信息
        log.info("远程调用分页查询 - URL: {}", url);
        log.info("远程调用分页查询 - 参数: gid={}, current={}, size={}", 
                requestParam.getGid(), requestParam.getCurrent(), requestParam.getSize());
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            
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

    @Override
    public Result<Void> saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        String url = projectServiceUrl + "/api/short-link/v1/recycle-bin/save";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<RecycleBinSaveReqDTO> entity = new HttpEntity<>(requestParam, headers);
        
        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            return JSON.parseObject(response, new TypeReference<Result<Void>>() {});
        } catch (Exception e) {
            log.error("远程调用保存回收站失败", e);
            throw new RuntimeException("远程调用保存回收站失败: " + e.getMessage());
        }
    }

    @Override
    public Result<ShortLinkPageResult> pageRecycleBinShortLink(ShortLinkPageReqDTO requestParam) {
        // 使用 UriComponentsBuilder 正确构建 URL，只添加非 null 参数
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(projectServiceUrl + "/api/short-link/v1/recycle-bin/page");
        
        if (requestParam.getGid() != null) {
            uriBuilder.queryParam("gid", requestParam.getGid());
        }
        if (requestParam.getCurrent() != null) {
            uriBuilder.queryParam("current", requestParam.getCurrent());
        }
        if (requestParam.getSize() != null) {
            uriBuilder.queryParam("size", requestParam.getSize());
        }
        
        String url = uriBuilder.toUriString();

        // 添加日志：打印请求信息
        log.info("远程调用分页查询 - URL: {}", url);
        log.info("远程调用分页查询 - 参数: gid={}, current={}, size={}",
                requestParam.getGid(), requestParam.getCurrent(), requestParam.getSize());

        try {
            String response = restTemplate.getForObject(url, String.class);

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

    /**
     * 恢复短链接
     * @param requestParam 请求参数： gid， fullShortUrl
     */
    @Override
    public Result<Void> recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {
        String url = projectServiceUrl + "/api/short-link/v1/recycle-bin/recover";
        
        log.info("远程调用恢复短链接开始 - URL: {}", url);
        log.info("远程调用恢复短链接 - 请求参数: {}", JSON.toJSONString(requestParam));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 传递用户信息到project服务
        String username = UserContext.getUsername();
        if (username != null) {
            headers.set("username", username);
            log.info("传递用户信息 - username: {}", username);
        }
        
        HttpEntity<RecycleBinRecoverReqDTO> entity = new HttpEntity<>(requestParam, headers);
        
        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            log.info("远程调用恢复短链接 - 响应: {}", response);
            log.info("远程调用恢复短链接成功");
            return JSON.parseObject(response, new TypeReference<Result<Void>>() {});
        } catch (Exception e) {
            log.error("远程调用恢复短链接失败", e);
            throw new RuntimeException("远程调用恢复短链接失败: " + e.getMessage());
        }
    }

} 