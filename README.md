# NovaLink - 分布式短链接服务系统

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0.7-brightgreen.svg)
![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5.3.1-blue.svg)
![ShardingSphere](https://img.shields.io/badge/ShardingSphere-5.3.2-red.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

**一个高性能、高可用的企业级短链接服务系统**

[功能特性](#-功能特性) • [技术架构](#-技术架构) • [快速开始](#-快速开始) • [项目结构](#-项目结构) • [核心功能](#-核心功能)

</div>

---

## 📖 项目简介

NovaLink 是一个基于 Spring Cloud 微服务架构的分布式短链接服务系统，为企业提供高效、安全、可靠的短链接管理平台。系统采用 SaaS 模式，支持多租户、高并发、大数据量场景，具备完善的缓存机制、分库分表、防缓存穿透等企业级特性。

### 核心价值

- 🚀 **高性能**：基于 Redis 缓存 + 布隆过滤器，支持高并发访问
- 🔒 **高可用**：分布式架构，支持水平扩展
- 📊 **数据统计**：完整的访问统计和分析功能
- 🛡️ **安全可靠**：防缓存穿透、防缓存击穿、数据加密存储
- 📈 **可扩展**：支持分库分表，轻松应对大数据量

---

## ✨ 功能特性

### 核心功能

- ✅ **短链接生成**：基于 Hash 算法的短链接生成，支持自定义域名
- ✅ **短链接跳转**：302 重定向，支持过期时间控制
- ✅ **短链接管理**：创建、修改、删除、分页查询
- ✅ **分组管理**：支持短链接分组，便于分类管理
- ✅ **回收站功能**：删除的短链接可恢复
- ✅ **访问统计**：点击量统计、访问数据分析

### 高级特性

- 🔍 **布隆过滤器**：防止缓存穿透，提升查询性能
- 🔐 **分布式锁**：基于 Redisson 实现，防止缓存击穿
- 💾 **缓存预热**：系统启动时自动预热热点数据
- 🎨 **Favicon 获取**：自动获取目标网站图标
- 📦 **分库分表**：基于 ShardingSphere 实现数据分片
- 🔄 **缓存策略**：多级缓存，动态 TTL 设置

---

## 🏗️ 技术架构

### 技术栈

| 分类 | 技术选型 |
|------|---------|
| **框架** | Spring Boot 3.0.7, Spring Cloud 2022.0.3 |
| **数据库** | MySQL 8.0+ |
| **缓存** | Redis + Redisson 3.21.3 |
| **ORM** | MyBatis-Plus 3.5.3.1 |
| **分库分表** | ShardingSphere 5.3.2 |
| **服务注册** | Nacos |
| **API 文档** | SpringDoc OpenAPI 3 |
| **工具类** | Hutool 5.8.20, Guava |
| **其他** | Jsoup (HTML 解析), FastJSON2 |

### 系统架构

```
┌─────────────┐
│   Gateway   │  API网关层
└──────┬──────┘
       │
   ┌───┴───┐
   │       │
┌──▼──┐ ┌─▼────┐
│Admin│ │Project│ 业务服务层
└──┬──┘ └─┬────┘
   │      │
   └──┬───┘
      │
┌─────▼─────┐
│  Redis    │  缓存层
└─────┬─────┘
      │
┌─────▼─────┐
│  MySQL    │  数据层 (分库分表)
└───────────┘
```

### 模块说明

- **admin**：后台管理服务，提供用户管理、短链接管理等功能
- **project**：短链接核心服务，提供短链接生成、跳转等核心功能
- **gateway**：API 网关，统一入口，路由转发

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.0+ (可选，用于服务注册发现)

### 安装步骤

1. **克隆项目**

```bash
git clone https://github.com/your-username/shortlink.git
cd shortlink
```

2. **配置数据库**

创建数据库并执行 SQL 脚本（位于 `数据库/` 目录）

3. **修改配置**

修改 `admin/src/main/resources/application.yaml` 和 `project/src/main/resources/application.yaml` 中的数据库和 Redis 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shortlink?useUnicode=true&characterEncoding=utf8
    username: your_username
    password: your_password
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: your_redis_password
```

4. **启动服务**

```bash
# 启动后台管理服务
cd admin
mvn spring-boot:run

# 启动短链接服务
cd project
mvn spring-boot:run
```

5. **访问服务**

- 后台管理服务：http://localhost:8902
- 短链接服务：http://localhost:8001
- API 文档：http://localhost:8001/swagger-ui.html

---

## 📁 项目结构

```
shortlink/
├── admin/                    # 后台管理模块
│   ├── controller/          # 控制器层
│   ├── service/             # 业务逻辑层
│   ├── dao/                 # 数据访问层
│   └── dto/                 # 数据传输对象
├── project/                 # 短链接核心模块
│   ├── controller/          # 控制器层
│   ├── service/             # 业务逻辑层
│   ├── dao/                 # 数据访问层
│   ├── config/              # 配置类
│   ├── toolkit/             # 工具类
│   └── job/                 # 定时任务
├── gateway/                 # API网关模块
├── 数据库/                   # 数据库脚本
├── 项目文档/                 # 项目文档
└── 流程图/                   # 系统流程图
```

---

## 🔧 核心功能

### 1. 短链接生成算法

采用 **Hash 算法 + 62 进制转换** 的方式生成短链接：

```java
// 核心算法：MurmurHash + Base62 编码
String shortUri = HashUtil.hashToBase62(originUrl + System.currentTimeMillis());
```

**特点**：
- 使用布隆过滤器快速判断短链接是否已存在
- 支持冲突检测和重试机制
- 最多重试 10 次，避免死循环

### 2. 短链接跳转流程

```
用户请求 → 布隆过滤器检查 → Redis 缓存查询 → 分布式锁 → 数据库查询 → 302 重定向
```

**优化策略**：
- ✅ 布隆过滤器：第一层防护，防止缓存穿透
- ✅ Redis 缓存：提升查询性能，减少数据库压力
- ✅ 分布式锁：防止缓存击穿，保证数据一致性
- ✅ 空值缓存：防止布隆过滤器误判导致的重复查询

### 3. 分库分表策略

基于 **ShardingSphere** 实现分库分表：

- **分片键**：`gid` (分组标识)
- **分片算法**：取模算法
- **路由表**：`t_link_goto` 表存储短链接与分组的映射关系

### 4. 缓存策略

- **缓存预热**：系统启动时自动加载热点数据
- **动态 TTL**：根据短链接有效期动态设置缓存过期时间
- **多级缓存**：Redis + 本地缓存（可选）

---

## 📊 数据库设计

### 核心表结构

**t_link** - 短链接表（分片表）
- 主键：`id`
- 分片键：`gid`
- 唯一索引：`full_short_url`

**t_link_goto** - 短链接路由表
- 存储短链接与分组的映射关系
- 用于跳转时快速定位数据所在分片

**t_user** - 用户表（分片表）
- 主键：`id`
- 分片键：`username`

---

## 🔐 安全特性

- ✅ **数据加密**：敏感数据（如密码）加密存储
- ✅ **JWT 认证**：基于 JWT 的用户认证机制
- ✅ **防缓存穿透**：布隆过滤器 + 空值缓存
- ✅ **防缓存击穿**：分布式锁保护
- ✅ **SQL 注入防护**：MyBatis-Plus 参数化查询

---

## 📈 性能优化

- **缓存命中率**：通过布隆过滤器和多级缓存提升命中率
- **数据库优化**：分库分表 + 索引优化
- **异步处理**：访问统计等非关键操作异步处理
- **连接池优化**：HikariCP 连接池配置优化

---

## 🧪 测试

### 接口测试

项目提供了完整的 API 文档，可通过 Swagger UI 进行测试：

```
http://localhost:8001/swagger-ui.html
```

### 单元测试

```bash
mvn test
```

---

## 📝 开发规范

### 代码结构

- **Controller**：处理 HTTP 请求，参数校验
- **Service**：业务逻辑处理
- **Mapper**：数据访问层，SQL 操作
- **DTO**：数据传输对象，分为 `req` 和 `resp` 包

### 命名规范

- 实体类：`XxxDO` (Data Object)
- 请求对象：`XxxReqDTO`
- 响应对象：`XxxRespDTO`
- Mapper 接口：`XxxMapper`
- Service 接口：`XxxService`
- Service 实现：`XxxServiceImpl`

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

---

## 👥 作者

- **Hanxuewei** - *初始开发* - [GitHub](https://github.com/hhhxxw)

---

## 🙏 致谢

- 感谢 [nageoffer](https://gitee.com/nageoffer/shortlink) 提供的原始项目参考
- 感谢所有贡献者的支持

---

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 [Issue](https://github.com/hhhxxw/novalink/issues)
- 发送邮件至：18962947617@163.com

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star！**


</div>
