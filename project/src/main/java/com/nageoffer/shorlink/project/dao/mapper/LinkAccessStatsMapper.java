package com.nageoffer.shorlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shorlink.project.dao.entity.LinkAccessStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 功能描述: 短链接基础访问监控的持久层
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/11/25
 */
public interface LinkAccessStatsMapper extends BaseMapper<LinkAccessStatsDO> {
    /**
     * 记录基础访问监控数据
     * 自定义SQL,这里为什么要用自定义SQL：如果记录不存在：执行 INSERT，如果记录已存在（唯一键冲突）：执行 UPDATE，累加统计值，这是 MySQL 的“插入或更新”语法，MyBatis-Plus 无法自动生成这种 SQL。
     * Param("linkAccessStats") 用于给方法参数命名，让 MyBatis 在 SQL 中通过该名称引用参数。
     */
    @Insert("INSERT INTO t_link_access_stats (full_short_url, gid, date, pv, uv, uip, hour, weekday, create_time, update_time, del_flag) " +
            "VALUES( #{linkAccessStats.fullShortUrl}, #{linkAccessStats.gid}, #{linkAccessStats.date}, #{linkAccessStats.pv}, #{linkAccessStats.uv}, #{linkAccessStats.uip}, #{linkAccessStats.hour}, #{linkAccessStats.weekday}, NOW(), NOW(), 0) ON DUPLICATE KEY UPDATE pv = pv +  #{linkAccessStats.pv}, " +
            "uv = uv + #{linkAccessStats.uv}, " +
            " uip = uip + #{linkAccessStats.uip};")
    void shortLinkStats(@Param("linkAccessStats") LinkAccessStatsDO linkAccessStatsDO);
}
