package com.nageoffer.shorlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shorlink.project.common.constant.RedisKeyConstant;
import com.nageoffer.shorlink.project.common.constant.ShortLinkConstant;
import com.nageoffer.shorlink.project.common.convention.exception.ClientException;
import com.nageoffer.shorlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shorlink.project.common.enums.ValidDateTypeEnum;
import com.nageoffer.shorlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shorlink.project.dao.entity.ShortLinkGotoDO;
import com.nageoffer.shorlink.project.dao.mapper.ShortLinkGotoMapper;
import com.nageoffer.shorlink.project.dao.mapper.ShortLinkMapper;
import com.nageoffer.shorlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shorlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shorlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shorlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shorlink.project.dto.resp.ShortLinkGroupCountRespDTO;
import com.nageoffer.shorlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shorlink.project.service.ShortLinkService;
import com.nageoffer.shorlink.project.toolkit.FaviconUtil;
import com.nageoffer.shorlink.project.toolkit.HashUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 功能描述: 短链接接口实现层
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/9/19
 */
@Slf4j
@Service
@AllArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {
    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ShortLinkMapper shortLinkMapper;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        // 生成短链接
        String shortLinkSuffix = generateSuffix(requestParam);
        // 拼接完整的短链接
        String fullShorUrl = StrBuilder.create(requestParam.getDomain())
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        
        // 处理有效期：如果是永久有效，设置为MySQL支持的最大日期
        Date validDate;
        if (requestParam.getValidDateType() != null && 
            requestParam.getValidDateType() == ValidDateTypeEnum.PERMANENT.getType()) {
            // 永久有效：设置为9999-12-31 23:59:59
            validDate = new Date(253402271999000L);
        } else {
            // 自定义有效期：使用传入的日期
            validDate = requestParam.getValidDate();
        }
        
        // 获取网站favicon
        String faviconUrl = FaviconUtil.getFaviconUrl(requestParam.getOriginUrl());
        log.debug("获取到favicon: {}", faviconUrl);
        
        // 组装ShortLinkDO, insert入库
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(validDate)
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(1)
                .fullShortUrl(fullShorUrl)
                .favicon(faviconUrl)
                .build();

        ShortLinkGotoDO linkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShorUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(linkGotoDO);
            // 插入成功后才加入布隆过滤器，防止插入失败时误加
            shortUriCreateCachePenetrationBloomFilter.add(fullShorUrl);
            
            // 创建成功后立即写入缓存（缓存预热）
            setToCache(fullShorUrl, shortLinkDO);
            log.debug("短链接创建成功，已预热缓存：{}", fullShorUrl);
        }catch (DuplicateKeyException ex){
            // 触发唯一键冲突：查询数据库确认是否真的存在
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShorUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            // 存在则抛出业务异常
            if(hasShortLinkDO != null){
                log.warn("短链接{}重复入库", fullShorUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    /**
     * 分页查询短链接
     * @param requestParam 分页查询短链接请求参数
     * @return
     */

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        // select * from t_link where gid = ? and enable_status = 1 and delFlag = 0 order by create_time desc;
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<ShortLinkGroupCountRespDTO> countByGidList(List<String> gidList) {
        // 输出参数
        log.info("接受到的批量查询请求, gidList: {}", gidList);

        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .select(ShortLinkDO::getGid)
                .in(ShortLinkDO::getGid, gidList)
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0);
        
        List<ShortLinkDO> shortLinkDOList = baseMapper.selectList(queryWrapper);
        log.info("查询到的短链接数量：{}", shortLinkDOList.size());
        log.info("短链接列表: {}", shortLinkDOList);
        // 按 gid 分组统计数量
        Map<String, Long> gidCountMap = shortLinkDOList.stream()
                .collect(Collectors.groupingBy(ShortLinkDO::getGid, Collectors.counting()));
        log.info("分组统计结果: {}", gidCountMap);
        // 转换为响应DTO，确保所有gid都有返回值（即使数量为0）
        List<ShortLinkGroupCountRespDTO> result = gidList.stream()
                .map(gid -> ShortLinkGroupCountRespDTO.builder()
                        .gid(gid)
                        .shortLinkCount(gidCountMap.getOrDefault(gid, 0L).intValue())
                        .build())
                .collect(Collectors.toList());
        log.info("最终返回结果: {}", result);
        return result;
    }

    /**
     * 短链接跳转（支持缓存+防击穿+防穿透）
     * @param shortUri 短链接后缀
     * @param request Http 请求
     * @param response Http 响应
     */
    @Override
    public void restoreUrl(String shortUri, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. 构建完整短链接
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String fullShortUrl = serverName + ":" + serverPort + "/" + shortUri;

        // 2. 风控：布隆过滤器检查（第一层防护 - 防止缓存穿透）
        if (!shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl)) {
            log.warn("布隆过滤器拦截 - 短链接不存在：{}", fullShortUrl);
            response.sendRedirect(ShortLinkConstant.PAGE_NOT_FOUND);
            return;
        }

        // 3. 检查是否命中空值缓存（第二层防护 - 防止布隆过滤器误判）
        if (isCachedAsNull(fullShortUrl)) {
            log.info("✅ 命中空值缓存：{}", fullShortUrl);
            response.sendRedirect(ShortLinkConstant.PAGE_NOT_FOUND);
            return;
        }

        // 4. 查询 Redis 正常缓存
        ShortLinkDO cachedShortLink = getFromCache(fullShortUrl);
        if (cachedShortLink != null) {
            log.info("✅ 缓存命中：{}", fullShortUrl);
            // 检查是否过期
            if (cachedShortLink.getValidDate() != null && cachedShortLink.getValidDate().before(new Date())) {
                log.warn("短链接已过期：{}, 过期时间：{}", fullShortUrl, cachedShortLink.getValidDate());
                response.sendRedirect(ShortLinkConstant.PAGE_EXPIRED);
                return;
            }
            // 异步更新访问统计
            baseMapper.incrementClickNum(cachedShortLink.getGid(), fullShortUrl);
            // 执行重定向
            response.sendRedirect(cachedShortLink.getOriginUrl());
            log.info("短链接跳转成功（缓存）：{} -> {}", fullShortUrl, cachedShortLink.getOriginUrl());
            return;
        }

        // 5. 缓存未命中，使用分布式锁防止缓存击穿
        String lockKey = RedisKeyConstant.getLockKey(fullShortUrl);
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁（等待3秒，持有10秒）
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    log.debug("🔒 获取分布式锁成功：{}", fullShortUrl);
                    
                    // 6. Double Check：空值缓存
                    if (isCachedAsNull(fullShortUrl)) {
                        log.info("✅ Double Check 命中空值缓存");
                        response.sendRedirect(ShortLinkConstant.PAGE_NOT_FOUND);
                        return;
                    }
                    
                    // 7. Double Check：正常缓存（可能其他线程已写入）
                    cachedShortLink = getFromCache(fullShortUrl);
                    if (cachedShortLink != null) {
                        log.info("✅ Double Check 缓存命中：{}", fullShortUrl);
                        // 检查是否过期
                        if (cachedShortLink.getValidDate() != null && cachedShortLink.getValidDate().before(new Date())) {
                            log.warn("短链接已过期：{}", fullShortUrl);
                            response.sendRedirect(ShortLinkConstant.PAGE_EXPIRED);
                            return;
                        }
                        baseMapper.incrementClickNum(cachedShortLink.getGid(), fullShortUrl);
                        response.sendRedirect(cachedShortLink.getOriginUrl());
                        return;
                    }
                    
                    // 8. 查询数据库
                    log.info("❌ 缓存未命中，查询数据库：{}", fullShortUrl);
                    ShortLinkDO shortLinkDO = queryFromDatabase(fullShortUrl);
                    
                    if (shortLinkDO == null) {
                        log.warn("数据库中未找到短链接：{}", fullShortUrl);
                        // ✅ 先缓存空值（防止布隆过滤器误判导致的重复查询）
                        cacheNullValue(fullShortUrl);
                        // 再重定向
                        response.sendRedirect(ShortLinkConstant.PAGE_NOT_FOUND);
                        return;
                    }
                    
                    // 9. 检查是否过期
                    if (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())) {
                        log.warn("短链接已过期：{}, 过期时间：{}", fullShortUrl, shortLinkDO.getValidDate());
                        response.sendRedirect(ShortLinkConstant.PAGE_EXPIRED);
                        return;
                    }
                    
                    // 10. 写入缓存（1小时）
                    setToCache(fullShortUrl, shortLinkDO);
                    
                    // 11. 更新访问统计
                    baseMapper.incrementClickNum(shortLinkDO.getGid(), fullShortUrl);
                    
                    // 12. 执行重定向
                    response.sendRedirect(shortLinkDO.getOriginUrl());
                    log.info("短链接跳转成功（数据库）：{} -> {}", fullShortUrl, shortLinkDO.getOriginUrl());
                    
                } finally {
                    lock.unlock();
                    log.debug("🔓 释放分布式锁：{}", fullShortUrl);
                }
            } else {
                // 获取锁超时 - 降级处理
                log.warn("⚠️ 获取分布式锁超时，降级查询：{}", fullShortUrl);
                ShortLinkDO shortLinkDO = queryFromDatabase(fullShortUrl);
                
                if (shortLinkDO == null) {
                    // ✅ 数据不存在，缓存空值
                    cacheNullValue(fullShortUrl);
                    response.sendRedirect(ShortLinkConstant.PAGE_NOT_FOUND);
                } else if (shortLinkDO.getValidDate() != null && 
                           shortLinkDO.getValidDate().before(new Date())) {
                    // 已过期
                    response.sendRedirect(ShortLinkConstant.PAGE_EXPIRED);
                } else {
                    // 正常跳转
                    baseMapper.incrementClickNum(shortLinkDO.getGid(), fullShortUrl);
                    response.sendRedirect(shortLinkDO.getOriginUrl());
                }
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁被中断：{}", fullShortUrl, e);
            Thread.currentThread().interrupt();
            response.sendRedirect(ShortLinkConstant.PAGE_ERROR);
        }
    }

    /**
     * 修改短链接
     * @param requestParam  修改短链接请求参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        log.info("修改短链接开始，id: {}", requestParam.getId());


        if(requestParam.getId() == null){
            throw new ServiceException("短链接ID不能为空");
        }

        if (requestParam.getGid() == null || requestParam.getGid().trim().isEmpty()) {
            throw new ServiceException("分组标识不能为空");
        }
        
        if (requestParam.getOriginalGid() == null || requestParam.getOriginalGid().trim().isEmpty()) {
            throw new ServiceException("原始分组标识不能为空");
        }

        if (requestParam.getFullShortUrl() == null || requestParam.getFullShortUrl().trim().isEmpty()) {
            throw new ServiceException("完整短链接不能为空");
        }

        // 1. 查询现有短链接记录（使用分片键）
        // ⚠️ 关键：必须使用 originalGid 查询，因为 gid 是分片键
        // 使用 originalGid + fullShortUrl 作为查询条件，只查询未删除的记录
        // select * from t_link t where gid = ? and full_short_url = ? and del_flag = 0
        log.info("查询短链接，originalGid: {}, fullShortUrl: {}", requestParam.getOriginalGid(), requestParam.getFullShortUrl());
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginalGid())  // 使用原始gid定位记录
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);

        // 检查短链接是否存在
        if(hasShortLinkDO == null){
            log.error("短链接记录不存在，originalGid: {}, fullShortUrl: {}", requestParam.getOriginalGid(), requestParam.getFullShortUrl());
            throw new ClientException("短链接记录不存在");
        }
        
        log.info("查询到短链接，id: {}, originalGid: {}, targetGid: {}", hasShortLinkDO.getId(), hasShortLinkDO.getGid(), requestParam.getGid());

        if(!Objects.equals(hasShortLinkDO.getId(), requestParam.getId())){
            throw new ServiceException("短链接ID不匹配，可能存在数据不一致");
        }

        // 构建更新对象
        // 设计思想：
        // 不可变字段：从原记录中复制
        // 可变字段：从请求参数中获取
        
        // 处理有效期：如果是永久有效，设置为MySQL支持的最大日期
        Date validDate;
        if (requestParam.getValidDateType() != null && 
            requestParam.getValidDateType() == ValidDateTypeEnum.PERMANENT.getType()) {
            // 永久有效：设置为9999-12-31 23:59:59
            validDate = new Date(253402271999000L);  // 9999-12-31 23:59:59
        } else {
            // 自定义有效期：使用传入的日期
            validDate = requestParam.getValidDate();
        }
        
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .id(hasShortLinkDO.getId())
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .clickNum(hasShortLinkDO.getClickNum())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(validDate)
                .build();
        // 根据gid是否变化，采用不同策略

        if(Objects.equals(requestParam.getOriginalGid(), requestParam.getGid())){
            // gid没有变化直接更新（originalGid == gid）
            log.info("gid没有变化，直接更新，id: {}, gid: {}", requestParam.getId(), requestParam.getGid());
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getId, requestParam.getId())
                    .eq(ShortLinkDO::getGid, requestParam.getOriginalGid())  // 使用原始gid定位记录
                    .eq(ShortLinkDO::getDelFlag, 0);
            int updateRows = baseMapper.update(shortLinkDO, updateWrapper);
            if(updateRows == 0){
                throw new ServiceException("更新短链接失败");
            }
            
            // 更新成功后，刷新缓存
            setToCache(requestParam.getFullShortUrl(), shortLinkDO);
            log.debug("短链接修改成功，已更新缓存：{}", requestParam.getFullShortUrl());
        } else {
            // gid已经变化，删除旧记录，插入新记录
            log.info("gid 已变化，执行删除+插入操作，旧gid: {}, 新gid：{}", requestParam.getOriginalGid(), requestParam.getGid());
            LambdaUpdateWrapper<ShortLinkDO> deleteWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getId,requestParam.getId())
                    .eq(ShortLinkDO::getGid, requestParam.getOriginalGid())  // 使用原始gid定位要删除的记录
                    .eq(ShortLinkDO::getDelFlag, 0);
            int deleteRows = baseMapper.delete(deleteWrapper);
            if(deleteRows == 0){
                throw new ServiceException("删除旧短链接记录失败");
            }

            int insertRows = baseMapper.insert(shortLinkDO);
            if(insertRows == 0){
                throw new ServiceException("插入新增短链接失败");
            }
            
            // gid变化后，也要更新缓存
            setToCache(requestParam.getFullShortUrl(), shortLinkDO);
            log.debug("短链接修改成功（gid变化），已更新缓存：{}", requestParam.getFullShortUrl());
        }
        log.info("修改短链接成功，id: {}", requestParam.getId());
    }

    /**
     * 生成短链接
     * @param requestParam 请求参数
     * @return 返回短链接
     */
    private String generateSuffix(ShortLinkCreateReqDTO requestParam){
        // 统计尝试次数，最多尝试10次
        int customGenerateCount = 0;
        String shortUri;
        while(true){
            if(customGenerateCount > 10){
                throw new ServiceException("短链接频繁生成， 请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            // 增加当前毫秒数的拼接，避免冲突
            originUrl += System.currentTimeMillis();
            // 下面这个短链接生成存在一定的冲突，上面增加毫秒就是为了尽量避免冲突
            shortUri = HashUtil.hashToBase62(originUrl);
            if(! shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)){
                break;
            }
            customGenerateCount ++;
        }
        return shortUri;

    }

    // ==================== 缓存相关辅助方法 ====================

    /**
     * 从 Redis 缓存查询短链接
     * @param fullShortUrl 完整短链接
     * @return 短链接对象，不存在返回 null
     */
    private ShortLinkDO getFromCache(String fullShortUrl) {
        try {
            String cacheKey = RedisKeyConstant.getShortLinkCacheKey(fullShortUrl);
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);

            if (cachedJson != null && !cachedJson.isEmpty() && !"null".equals(cachedJson)) {
                // 正常的JSON数据
                return JSON.parseObject(cachedJson, ShortLinkDO.class);
            }
        } catch (Exception e) {
            log.error("从缓存读取短链接失败：{}", fullShortUrl, e);
        }
        return null;
    }
    
    /**
     * 将短链接写入 Redis 缓存
     * @param fullShortUrl 完整短链接
     * @param shortLinkDO 短链接对象
     */
    private void setToCache(String fullShortUrl, ShortLinkDO shortLinkDO) {
        try {
            String cacheKey = RedisKeyConstant.getShortLinkCacheKey(fullShortUrl);
            String jsonValue = JSON.toJSONString(shortLinkDO);
            
            // 使用 LinkUtil 计算动态缓存有效期
            long cacheValidTime = com.nageoffer.shorlink.project.toolkit.LinkUtil.getLinkCacheValidDate(shortLinkDO.getValidDate());
            
            // 设置缓存，TTL = 动态计算的有效期
            stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, cacheValidTime, TimeUnit.MILLISECONDS);
            log.debug("写入缓存成功：{}，TTL={}ms", fullShortUrl, cacheValidTime);
        } catch (Exception e) {
            log.error("写入缓存失败：{}", fullShortUrl, e);
        }
    }
    
    /**
     * 从数据库查询短链接（抽取的公共方法）
     * @param fullShortUrl 完整短链接
     * @return 短链接对象，不存在返回 null
     */
    private ShortLinkDO queryFromDatabase(String fullShortUrl) {
        try {
            // 1. 查询路由表获取 gid
            LambdaQueryWrapper<ShortLinkGotoDO> gotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(gotoQueryWrapper);
            
            if (shortLinkGotoDO == null) {
                log.warn("路由表中未找到短链接：{}", fullShortUrl);
                return null;
            }
            
            // 2. 根据 gid 查询短链接详情（自动路由到对应分片）
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getEnableStatus, 1)
                    .eq(ShortLinkDO::getDelFlag, 0);
            
            return baseMapper.selectOne(queryWrapper);
        } catch (Exception e) {
            log.error("数据库查询短链接失败：{}", fullShortUrl, e);
            return null;
        }
    }

    /**
     * 缓存空值（防止布隆过滤器误判导致的缓存穿透）
     * @param fullShortUrl 完整短链接
     */
    private void cacheNullValue(String fullShortUrl) {
        try {
            String cacheKey = RedisKeyConstant.getShortLinkCacheKey(fullShortUrl);

            // 缓存一个特殊标记（空对象）
            String nullMarker = "null";

            // 设置较短的过期时间（5分钟），避免占用过多内存
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    nullMarker,
                    5,
                    TimeUnit.MINUTES
            );

            log.debug("缓存空值成功：{}, TTL=5分钟", fullShortUrl);
        } catch (Exception e) {
            log.error("缓存空值失败：{}", fullShortUrl, e);
        }
    }
    
    /**
     * 判断是否缓存了空值
     * @param fullShortUrl 完整短链接
     * @return true-命中空值缓存，false-未命中
     */
    private boolean isCachedAsNull(String fullShortUrl) {
        try {
            String cacheKey = RedisKeyConstant.getShortLinkCacheKey(fullShortUrl);
            String value = stringRedisTemplate.opsForValue().get(cacheKey);
            return "null".equals(value);
        } catch (Exception e) {
            log.error("检查空值缓存失败：{}", fullShortUrl, e);
            return false;
        }
    }
}
