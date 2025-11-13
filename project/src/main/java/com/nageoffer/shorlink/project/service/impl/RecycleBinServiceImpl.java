package com.nageoffer.shorlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shorlink.project.common.constant.RedisKeyConstant;
import com.nageoffer.shorlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shorlink.project.dao.mapper.ShortLinkMapper;
import com.nageoffer.shorlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shorlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shorlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shorlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 功能描述: 回收站管理接口实现层
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * 保存回收站
     * @param requestParam 请求参数
     */
    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        // 1. 更新数据库：将短链接状态改为未启用（回收站）
        // UPDATE t_link SET enable_status = 0 WHERE full_short_url = ? AND gid = ? AND enable_status = 1 AND del_flag = 0;
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                // 将状态置零标识移动到回收站 TODO 学习建造者模式
                .enableStatus(0)
                .build();
        int updateCount = baseMapper.update(shortLinkDO, updateWrapper);
        
        // 2. 删除缓存：如果更新成功，则删除对应的Redis缓存
        if (updateCount > 0) {
            String cacheKey = RedisKeyConstant.getShortLinkCacheKey(requestParam.getFullShortUrl());
            Boolean deleteResult = stringRedisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleteResult)) {
                log.info("✅ 回收站保存成功，已删除缓存：{}", requestParam.getFullShortUrl());
            } else {
                log.warn("⚠️ 回收站保存成功，但缓存删除失败或缓存不存在：{}", requestParam.getFullShortUrl());
            }
        } else {
            log.warn("⚠️ 回收站保存失败：短链接不存在或已在回收站中：{}", requestParam.getFullShortUrl());
        }
    }

    /**
     * 分页查询短链接
     * @param requestParam 分页查询短链接请求参数
     * @return
     */

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        // select * from t_link where gid = ? and enable_status = 0 and delFlag = 0 order by create_time desc;
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                // 查询未激活状态 - 0
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
            result.setDomain("http://" + result.getDomain());
            return result;
        });
    }
}
