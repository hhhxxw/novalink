package com.nageoffer.shorlink.project.common.constant;

/**
 * <p>
 * 功能描述: Redis Key 常量类
 * </p>
 *
 * @author Hanxuewei
 * @since 2025/10/10
 */
public class RedisKeyConstant {

    /**
     * 短链接跳转前缀 Key
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link:goto:%s";

    /**
     * 短链接空值跳转前缀 Key
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link:is-null:goto_%s";

    /**
     * 布隆过滤器：短链接创建缓存穿透
     * 用于防止查询不存在的短链接时穿透到数据库
     */
    public static final String SHORT_LINK_CREATE_BLOOM_FILTER = "short_link:bloom_filter:create";
    
    /**
     * 短链接缓存前缀
     * 格式：short_link:cache:{fullShortUrl}
     * 示例：short_link:cache:localhost:8001/abc123
     * 用途：缓存短链接详情，减少数据库查询
     */
    public static final String SHORT_LINK_CACHE_PREFIX = "short_link:cache:";
    
    /**
     * 短链接访问统计前缀
     * 格式：short_link:stats:{shortUri}
     * 示例：short_link:stats:abc123
     * 值类型：Hash
     * 字段：pv(访问量)、uv(独立访客)、today_pv、today_uv
     */
    public static final String SHORT_LINK_STATS_PREFIX = "short_link:stats:";
    
    /**
     * 短链接 UV 统计（独立访客）
     * 格式：short_link:uv:{shortUri}
     * 示例：short_link:uv:abc123
     * 值类型：HyperLogLog
     * 用途：统计独立访客数量
     */
    public static final String SHORT_LINK_UV_PREFIX = "short_link:uv:";
    
    /**
     * 短链接今日 UV 统计
     * 格式：short_link:uv:{shortUri}:{date}
     * 示例：short_link:uv:abc123:20251010
     * 值类型：HyperLogLog
     * 用途：统计每日独立访客，按天分开存储
     */
    public static final String SHORT_LINK_UV_DAILY_PREFIX = "short_link:uv:daily:";
    
    /**
     * 访问频率限流
     * 格式：rate:limit:ip:{ip}
     * 示例：rate:limit:ip:192.168.1.1
     * 值类型：String (计数器)
     * 用途：限制单个IP的访问频率
     * TTL：60秒
     */
    public static final String RATE_LIMIT_IP_PREFIX = "rate:limit:ip:";
    
    /**
     * 短链接访问记录（去重）
     * 格式：short_link:access:{shortUri}:{date}
     * 示例：short_link:access:abc123:20251010
     * 值类型：Set
     * 用途：记录每天访问过的IP，用于去重统计
     * TTL：7天
     */
    public static final String SHORT_LINK_ACCESS_RECORD_PREFIX = "short_link:access:";
    
    /**
     * 短链接跳转分布式锁
     * 格式：short_link:lock:{fullShortUrl}
     * 示例：short_link:lock:localhost:8001/abc123
     * 用途：防止并发更新点击统计时的数据不一致
     * TTL：10秒
     */
    public static final String SHORT_LINK_LOCK_PREFIX = "short_link:lock:";
    
    /**
     * 热点短链接排行榜
     * 格式：short_link:hot:rank
     * 值类型：ZSet
     * 成员：shortUri
     * 分数：访问量
     * 用途：维护访问量 Top N 的短链接
     */
    public static final String SHORT_LINK_HOT_RANK = "short_link:hot:rank";
    
    /**
     * 短链接访问日志队列
     * 格式：short_link:log:queue
     * 值类型：List
     * 用途：异步记录访问日志，由消费者批量写入数据库
     */
    public static final String SHORT_LINK_LOG_QUEUE = "short_link:log:queue";
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取短链接缓存 Key
     * @param fullShortUrl 完整短链接（如：localhost:8001/abc123）
     * @return 缓存 Key
     */
    public static String getShortLinkCacheKey(String fullShortUrl) {
        return SHORT_LINK_CACHE_PREFIX + fullShortUrl;
    }
    
    /**
     * 获取短链接统计 Key
     * @param shortUri 短链接后缀（如：abc123）
     * @return 统计 Key
     */
    public static String getStatsKey(String shortUri) {
        return SHORT_LINK_STATS_PREFIX + shortUri;
    }
    
    /**
     * 获取 UV 统计 Key
     * @param shortUri 短链接后缀
     * @return UV 统计 Key
     */
    public static String getUvKey(String shortUri) {
        return SHORT_LINK_UV_PREFIX + shortUri;
    }
    
    /**
     * 获取每日 UV 统计 Key
     * @param shortUri 短链接后缀
     * @param date 日期（格式：yyyyMMdd）
     * @return 每日 UV 统计 Key
     */
    public static String getDailyUvKey(String shortUri, String date) {
        return SHORT_LINK_UV_DAILY_PREFIX + shortUri + ":" + date;
    }
    
    /**
     * 获取 IP 限流 Key
     * @param ip IP 地址
     * @return 限流 Key
     */
    public static String getRateLimitKey(String ip) {
        return RATE_LIMIT_IP_PREFIX + ip;
    }
    
    /**
     * 获取访问记录 Key
     * @param shortUri 短链接后缀
     * @param date 日期（格式：yyyyMMdd）
     * @return 访问记录 Key
     */
    public static String getAccessRecordKey(String shortUri, String date) {
        return SHORT_LINK_ACCESS_RECORD_PREFIX + shortUri + ":" + date;
    }
    
    /**
     * 获取分布式锁 Key
     * @param fullShortUrl 完整短链接
     * @return 锁 Key
     */
    public static String getLockKey(String fullShortUrl) {
        return SHORT_LINK_LOCK_PREFIX + fullShortUrl;
    }
}