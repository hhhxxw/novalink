# MySQL 连接错误：Public Key Retrieval is not allowed

## 📋 错误信息

```
java.sql.SQLNonTransientConnectionException: Public Key Retrieval is not allowed
Caused by: com.mysql.cj.exceptions.UnableToConnectException: Public Key Retrieval is not allowed
```

---

## 🔍 错误原因深度分析

### 1. MySQL 8.0 认证机制变化

| MySQL 版本 | 默认认证插件 | 密码传输方式 | 安全性 |
|-----------|------------|------------|--------|
| MySQL 5.7 及之前 | `mysql_native_password` | SHA1 哈希 | ⭐⭐⭐ |
| MySQL 8.0+ | `caching_sha2_password` | RSA 公钥加密 | ⭐⭐⭐⭐⭐ |

### 2. 连接流程对比

#### MySQL 5.7 连接流程
```
客户端 → 发送用户名
MySQL → 返回 salt（随机数）
客户端 → 计算 SHA1(password + salt)
MySQL → 验证成功 ✅
```

#### MySQL 8.0 连接流程（caching_sha2_password）
```
客户端 → 发送用户名
MySQL → 返回 salt + 认证方式(caching_sha2_password)
客户端 → 尝试 SHA256(password + salt)
MySQL → 检查缓存

如果缓存命中：
  → 验证成功 ✅

如果缓存未命中（首次连接）：
  → MySQL: "需要公钥加密"
  → 客户端: "请求公钥"
  
  此时检查 JDBC 配置：
    allowPublicKeyRetrieval=false（默认）
      → ❌ 抛出异常: Public Key Retrieval is not allowed
    
    allowPublicKeyRetrieval=true
      → MySQL 返回公钥
      → 客户端用公钥加密密码
      → 验证成功 ✅
```

### 3. 为什么默认禁止公钥获取？

**安全考虑：**
- 防止中间人攻击（MITM）
- 不安全的网络环境下，传输公钥可能被劫持
- 强制使用 SSL/TLS 加密连接

**官方推荐：**
```
生产环境：useSSL=true + 不需要 allowPublicKeyRetrieval
开发环境：allowPublicKeyRetrieval=true（便利性）
```

---

## 🛠️ 解决方案对比

### 方案一：允许公钥获取（快速修复）✅

**修改 jdbcUrl：**
```yaml
jdbcUrl: jdbc:mysql://127.0.0.1:3306/link?
  allowPublicKeyRetrieval=true&
  useSSL=false
```

**优点：**
- ✅ 立即解决问题
- ✅ 本地开发方便
- ✅ 不需要修改MySQL配置

**缺点：**
- ⚠️ 有安全风险（公网环境）
- ⚠️ 不推荐用于生产环境

**适用场景：**
- 本地开发环境
- 内网环境
- 快速调试

---

### 方案二：使用 SSL 连接（生产推荐）🔒

**修改 jdbcUrl：**
```yaml
jdbcUrl: jdbc:mysql://127.0.0.1:3306/link?
  useSSL=true&
  requireSSL=true&
  verifyServerCertificate=false  # 开发环境可用
```

**MySQL 服务器配置：**
```sql
-- 检查 SSL 是否启用
SHOW VARIABLES LIKE '%ssl%';

-- 如果未启用，需要配置 my.cnf
[mysqld]
ssl-ca=/path/to/ca.pem
ssl-cert=/path/to/server-cert.pem
ssl-key=/path/to/server-key.pem
```

**优点：**
- ✅ 高安全性
- ✅ 符合生产标准
- ✅ 防止中间人攻击

**缺点：**
- ⚠️ 需要配置证书
- ⚠️ 配置相对复杂

---

### 方案三：修改MySQL用户认证插件（兼容方案）

**修改用户认证方式：**
```sql
-- 方法1：修改现有用户
ALTER USER 'root'@'localhost' 
IDENTIFIED WITH mysql_native_password BY 'your_password';

-- 方法2：创建新用户
CREATE USER 'shortlink'@'%' 
IDENTIFIED WITH mysql_native_password BY 'your_password';
GRANT ALL PRIVILEGES ON link.* TO 'shortlink'@'%';
FLUSH PRIVILEGES;
```

**优点：**
- ✅ 兼容旧版驱动
- ✅ 不需要修改 JDBC URL

**缺点：**
- ⚠️ 降低了安全性
- ⚠️ 不推荐长期使用

---

## 📊 JDBC URL 参数完整说明

### 已修复的配置
```yaml
jdbcUrl: jdbc:mysql://127.0.0.1:3306/link?
  useUnicode=true&                    # 使用Unicode编码
  characterEncoding=UTF-8&            # 字符集UTF-8
  rewriteBatchedStatements=true&      # 批量操作优化
  allowMultiQueries=true&             # 允许多语句
  serverTimezone=Asia/Shanghai&       # 时区设置
  allowPublicKeyRetrieval=true&       # ✅ 允许获取公钥（新增）
  useSSL=false                        # ✅ 关闭SSL（开发环境）
```

### 参数详解

| 参数 | 作用 | 推荐值 | 说明 |
|-----|------|-------|------|
| `allowPublicKeyRetrieval` | 是否允许客户端获取公钥 | 开发: true<br>生产: false | MySQL 8.0 必需 |
| `useSSL` | 是否使用SSL连接 | 开发: false<br>生产: true | 加密传输 |
| `requireSSL` | 强制SSL | 生产: true | 必须SSL连接 |
| `verifyServerCertificate` | 验证服务器证书 | 生产: true | 防止伪造服务器 |
| `serverTimezone` | 服务器时区 | Asia/Shanghai | 避免时区错误 |
| `useUnicode` | 使用Unicode | true | 支持中文 |
| `characterEncoding` | 字符编码 | UTF-8 | 字符集 |
| `autoReconnect` | 自动重连 | true | 连接断开重连 |
| `maxReconnects` | 最大重连次数 | 3 | 重连限制 |

---

## 🔄 完整解决步骤

### 步骤 1：确认错误 ✅
```
日志关键字：
- Public Key Retrieval is not allowed
- caching_sha2_password
- UnableToConnectException
```

### 步骤 2：检查 MySQL 版本
```bash
mysql --version
# 或在 MySQL 中执行
SELECT VERSION();
```

如果是 **MySQL 8.0+**，确认是这个问题 ✅

### 步骤 3：选择解决方案

**开发环境（快速）：**
```yaml
# shardingsphere-config-dev.yaml
jdbcUrl: jdbc:mysql://127.0.0.1:3306/link?...&allowPublicKeyRetrieval=true&useSSL=false
```

**生产环境（安全）：**
```yaml
# shardingsphere-config-prod.yaml
jdbcUrl: jdbc:mysql://生产IP:3306/link?...&useSSL=true&requireSSL=true
```

### 步骤 4：重启应用
```bash
# 停止应用
# 重新运行
```

### 步骤 5：验证连接
```
启动日志应该显示：
✅ HikariPool-1 - Starting...
✅ HikariPool-1 - Start completed.

而不是：
❌ HikariPool-1 - Exception during pool initialization
```

---

## 📝 本次修复记录

### 已修改文件

1. **admin/src/main/resources/shardingsphere-config-dev.yaml**
   - ✅ 添加 `allowPublicKeyRetrieval=true&useSSL=false`

2. **admin/src/main/resources/shardingsphere-config-prod.yaml**
   - ✅ 添加 `allowPublicKeyRetrieval=true&useSSL=false`

3. **project/src/main/resources/shardingsphere-config-dev.yaml**
   - ✅ 添加 `allowPublicKeyRetrieval=true&useSSL=false`

4. **project/src/main/resources/shardingsphere-config-prod.yaml**
   - ✅ 添加 `allowPublicKeyRetrieval=true&useSSL=false`

### 修改前后对比

**修改前：**
```yaml
jdbcUrl: jdbc:mysql://127.0.0.1:3306/link?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&allowMultiQueries=true&serverTimezone=Asia/Shanghai
```

**修改后：**
```yaml
jdbcUrl: jdbc:mysql://127.0.0.1:3306/link?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&allowMultiQueries=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
```

---

## ⚠️ 生产环境建议

### 当前配置的安全隐患

```yaml
# ⚠️ 开发配置，不适合生产
jdbcUrl: jdbc:mysql://127.0.0.1:3306/link?
  allowPublicKeyRetrieval=true  # ⚠️ 安全风险
  useSSL=false                   # ⚠️ 明文传输
```

### 生产环境推荐配置

```yaml
# ✅ 生产配置建议
dataSources:
  ds_0:
    jdbcUrl: jdbc:mysql://${DB_HOST}:3306/link?
      useUnicode=true&
      characterEncoding=UTF-8&
      rewriteBatchedStatements=true&
      allowMultiQueries=true&
      serverTimezone=Asia/Shanghai&
      useSSL=true&                      # ✅ 启用SSL
      requireSSL=true&                  # ✅ 强制SSL
      verifyServerCertificate=true      # ✅ 验证证书
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 安全检查清单

- [ ] 使用SSL加密连接
- [ ] 使用专用数据库用户（非root）
- [ ] 密码使用环境变量
- [ ] 限制用户访问权限
- [ ] 定期更新密码
- [ ] 监控异常连接

---

## 🎓 知识点总结

### 1. 核心问题
- MySQL 8.0 默认使用 `caching_sha2_password` 认证
- JDBC 默认禁止获取公钥（安全考虑）
- 首次连接需要公钥加密密码

### 2. 解决思路
- **快速方案**：允许公钥获取（`allowPublicKeyRetrieval=true`）
- **安全方案**：使用SSL连接（`useSSL=true`）
- **兼容方案**：改用旧认证插件（不推荐）

### 3. 最佳实践
- **开发环境**：`allowPublicKeyRetrieval=true&useSSL=false`（便利）
- **生产环境**：`useSSL=true&requireSSL=true`（安全）
- **敏感数据**：使用环境变量，不硬编码

### 4. 排查技巧
- 看错误信息关键字
- 确认 MySQL 版本
- 检查 JDBC URL 参数
- 验证数据库用户权限

---

## 📚 延伸阅读

### 官方文档
- [MySQL 8.0 认证插件](https://dev.mysql.com/doc/refman/8.0/en/caching-sha2-pluggable-authentication.html)
- [MySQL Connector/J 配置](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-configuration-properties.html)

### 相关问题
- SSL 证书配置
- 连接池优化
- 多环境配置管理

### 常见错误
- `Public Key Retrieval is not allowed` ← **本次问题**
- `Access denied for user`
- `Communications link failure`
- `The server time zone value 'XXX' is unrecognized`

---

## ✅ 下一步操作

1. **重启 admin 模块**
   ```bash
   # 使用 prod 配置启动
   VM Options: -Ddatabase.env=prod
   ```

2. **检查启动日志**
   ```
   应该看到：
   ✅ HikariPool-1 - Start completed
   ✅ Tomcat started on port(s): 8902
   ```

3. **测试接口**
   ```bash
   # 测试连接是否正常
   curl http://localhost:8902/api/short-link/admin/v1/page?gid=15&current=1&size=10
   ```

4. **如果还有问题**
   - 检查 MySQL 是否启动
   - 检查端口 3306 是否开放
   - 检查用户名密码是否正确
   - 检查数据库 `link` 是否存在

---

**问题已修复！** 🎉

现在你可以重新启动应用了，应该不会再出现 "Public Key Retrieval is not allowed" 错误。 