# Apifox 修改短链接接口测试指南

## 📋 测试目标

验证修改短链接接口在分库分表场景下能否正确处理：
1. ✅ **场景1**：同组内修改（originalGid == gid）
2. ✅ **场景2**：跨组移动（originalGid != gid）

---

## 🚀 测试前准备

### 1. 启动服务

#### 方式一：使用IDEA启动
```
1. 先启动 project 服务
   - 运行 project/src/main/java/.../ShortLinkApplication.java
   - 确认启动成功：http://127.0.0.1:8001

2. 再启动 admin 服务
   - 运行 admin/src/main/java/.../ShortLinkAdminApplication.java
   - 确认启动成功：http://127.0.0.1:8902
```

#### 方式二：使用Maven命令
```bash
# 终端1：启动 project 服务
cd project
mvn spring-boot:run

# 终端2：启动 admin 服务
cd admin
mvn spring-boot:run
```

### 2. 检查服务状态

确认两个服务都已启动：
- ✅ project服务：http://127.0.0.1:8001
- ✅ admin服务：http://127.0.0.1:8902

---

## 📊 测试数据准备

### 步骤1：创建测试短链接

#### 在Apifox中创建请求

**请求配置**：
- **名称**：创建短链接（用于测试）
- **方法**：`POST`
- **URL**：`http://127.0.0.1:8902/api/short-link/admin/v1/create`

**Headers**：
```
Content-Type: application/json
```

**Body（选择 JSON）**：
```json
{
  "domain": "nurl.ink",
  "originUrl": "https://www.example.com/test-update",
  "gid": "test-group-A",
  "createdType": 0,
  "validDateType": 0,
  "describe": "用于测试修改功能的短链接"
}
```

**点击「发送」**

**预期响应**：
```json
{
  "code": "0",
  "message": "success",
  "data": {
    "gid": "test-group-A",
    "originUrl": "https://www.example.com/test-update",
    "fullShortUrl": "nurl.ink/xxxxxx"
  }
}
```

**📌 重要：记录返回的数据！**
```
✅ 记录 fullShortUrl：nurl.ink/xxxxxx
✅ 记录 gid：test-group-A
```

### 步骤2：查询短链接获取ID

**请求配置**：
- **名称**：查询短链接（获取ID）
- **方法**：`GET`
- **URL**：`http://127.0.0.1:8902/api/short-link/admin/v1/page`

**Query参数**：
| 参数名 | 参数值 | 说明 |
|--------|--------|------|
| gid | test-group-A | 分组标识 |
| current | 1 | 当前页码 |
| size | 10 | 每页条数 |

**点击「发送」**

**预期响应**：
```json
{
  "code": "0",
  "message": "success",
  "data": {
    "records": [
      {
        "id": 123456789,  // ⭐ 记录这个ID！
        "domain": "nurl.ink",
        "shortUri": "xxxxxx",
        "fullShortUrl": "nurl.ink/xxxxxx",
        "originUrl": "https://www.example.com/test-update",
        "gid": "test-group-A",
        "validDateType": 0,
        "describe": "用于测试修改功能的短链接",
        "createTime": "2025-10-05 12:00:00"
      }
    ],
    "total": 1
  }
}
```

**📌 记录以下信息（后面测试要用）**：
```
✅ id: 123456789
✅ fullShortUrl: nurl.ink/xxxxxx
✅ originalGid: test-group-A
```

---

## 🧪 测试场景一：同组内修改

**测试目的**：验证不改变分组的情况下，能否正常修改短链接。

### Apifox配置

**请求配置**：
- **名称**：修改短链接 - 同组修改
- **方法**：`PUT`
- **URL**：`http://127.0.0.1:8902/api/short-link/admin/v1/update`

**Headers**：
```
Content-Type: application/json
```

**Body（选择 JSON）**：
```json
{
  "id": 123456789,
  "originalGid": "test-group-A",
  "gid": "test-group-A",
  "fullShortUrl": "nurl.ink/xxxxxx",
  "originUrl": "https://www.updated-example.com",
  "describe": "已修改：同组内更新测试",
  "validDateType": 0
}
```

**⚠️ 注意替换**：
- `id`：替换为步骤2中查询到的ID
- `fullShortUrl`：替换为实际的短链接
- `originalGid` 和 `gid` 保持一致（都是 test-group-A）

### 点击「发送」

**预期响应**：
```json
{
  "code": "0",
  "message": "success",
  "data": null
}
```

### 验证结果

#### 验证方式1：通过API查询
重新调用查询接口：
```
GET http://127.0.0.1:8902/api/short-link/admin/v1/page?gid=test-group-A&current=1&size=10
```

**检查点**：
- ✅ `originUrl` 已更新为 `https://www.updated-example.com`
- ✅ `describe` 已更新为 `已修改：同组内更新测试`
- ✅ `gid` 仍然是 `test-group-A`

#### 验证方式2：查看后台日志
在 project 服务的控制台中查看日志：
```
修改短链接开始，id: 123456789
查询短链接，originalGid: test-group-A, fullShortUrl: nurl.ink/xxxxxx
查询到短链接，id: 123456789, originalGid: test-group-A, targetGid: test-group-A
gid没有变化，直接更新，id: 123456789, gid: test-group-A
修改短链接成功，id: 123456789
```

#### 验证方式3：数据库查询
```sql
-- 根据 gid 计算分片，查看对应分片表
-- 假设 test-group-A 路由到 t_link_5
SELECT * FROM t_link_5 
WHERE full_short_url = 'nurl.ink/xxxxxx' 
  AND del_flag = 0;

-- 应该看到：
-- - origin_url = 'https://www.updated-example.com'
-- - describe = '已修改：同组内更新测试'
```

---

## 🧪 测试场景二：跨组移动

**测试目的**：验证改变分组时，能否正确执行跨分片迁移（删除旧记录+插入新记录）。

### Apifox配置

**请求配置**：
- **名称**：修改短链接 - 跨组移动
- **方法**：`PUT`
- **URL**：`http://127.0.0.1:8902/api/short-link/admin/v1/update`

**Headers**：
```
Content-Type: application/json
```

**Body（选择 JSON）**：
```json
{
  "id": 123456789,
  "originalGid": "test-group-A",
  "gid": "test-group-B",
  "fullShortUrl": "nurl.ink/xxxxxx",
  "originUrl": "https://www.updated-example.com",
  "describe": "已修改：跨组移动测试",
  "validDateType": 1,
  "validDate": "2025-12-31 23:59:59"
}
```

**⚠️ 注意关键字段**：
- `originalGid`: `test-group-A`（原始分组）
- `gid`: `test-group-B`（目标分组，不同于原始分组）
- 这会触发跨分片迁移逻辑！

### 点击「发送」

**预期响应**：
```json
{
  "code": "0",
  "message": "success",
  "data": null
}
```

### 验证结果

#### 验证方式1：在新分组中查询
```
GET http://127.0.0.1:8902/api/short-link/admin/v1/page?gid=test-group-B&current=1&size=10
```

**检查点**：
- ✅ 能够查询到该短链接
- ✅ `gid` 已更新为 `test-group-B`
- ✅ `describe` 已更新为 `已修改：跨组移动测试`
- ✅ `validDateType` 已更新为 `1`

#### 验证方式2：在旧分组中查询
```
GET http://127.0.0.1:8902/api/short-link/admin/v1/page?gid=test-group-A&current=1&size=10
```

**检查点**：
- ✅ 查询不到该短链接（已从旧分组移除）

#### 验证方式3：查看后台日志
在 project 服务的控制台中查看日志：
```
修改短链接开始，id: 123456789
查询短链接，originalGid: test-group-A, fullShortUrl: nurl.ink/xxxxxx
查询到短链接，id: 123456789, originalGid: test-group-A, targetGid: test-group-B
gid 已变化，执行删除+插入操作，旧gid: test-group-A, 新gid：test-group-B
修改短链接成功，id: 123456789
```

**关键日志**：
- ✅ 使用 `originalGid` 查询
- ✅ 检测到 gid 变化
- ✅ 执行删除+插入操作

#### 验证方式4：数据库查询
```sql
-- 查询旧分片表（假设 test-group-A 路由到 t_link_5）
SELECT * FROM t_link_5 
WHERE full_short_url = 'nurl.ink/xxxxxx' 
  AND del_flag = 0;
-- 应该查不到或已标记删除

-- 查询新分片表（假设 test-group-B 路由到 t_link_12）
SELECT * FROM t_link_12 
WHERE full_short_url = 'nurl.ink/xxxxxx' 
  AND del_flag = 0;
-- 应该能查到，且 gid = 'test-group-B'
```

---

## 🔧 如何确定数据在哪个分片表？

### 方法1：查看SQL日志

在 `project/src/main/resources/shardingsphere-config-dev.yaml` 中开启SQL日志：
```yaml
props:
  sql-show: true  # 确保这个配置是 true
```

重启 project 服务后，执行查询，控制台会显示：
```
Logic SQL: SELECT * FROM t_link WHERE gid = ? AND ...
Actual SQL: ds_0 ::: SELECT * FROM t_link_5 WHERE gid = ? AND ...
                                    ^^^^^^^^^
                                这就是实际的分片表！
```

### 方法2：直接查询所有分片表

```sql
-- 批量查询所有分片表
SELECT 
    't_link_0' as table_name, id, gid, full_short_url, origin_url 
FROM t_link_0 WHERE full_short_url = 'nurl.ink/xxxxxx'
UNION ALL
SELECT 
    't_link_1' as table_name, id, gid, full_short_url, origin_url 
FROM t_link_1 WHERE full_short_url = 'nurl.ink/xxxxxx'
UNION ALL
-- ... 重复到 t_link_15
SELECT 
    't_link_15' as table_name, id, gid, full_short_url, origin_url 
FROM t_link_15 WHERE full_short_url = 'nurl.ink/xxxxxx';
```

---

## ❌ 测试错误场景（可选）

### 测试1：不传 originalGid
```json
{
  "id": 123456789,
  "gid": "test-group-B",
  "fullShortUrl": "nurl.ink/xxxxxx",
  "originUrl": "https://www.example.com"
}
```

**预期响应**：
```json
{
  "code": "1",
  "message": "原始分组标识不能为空"
}
```

### 测试2：originalGid 传错
```json
{
  "id": 123456789,
  "originalGid": "wrong-group",  // 错误的原始分组
  "gid": "test-group-B",
  "fullShortUrl": "nurl.ink/xxxxxx",
  "originUrl": "https://www.example.com"
}
```

**预期响应**：
```json
{
  "code": "1",
  "message": "短链接记录不存在"
}
```

---

## 📝 Apifox 环境变量配置（推荐）

为了方便测试，可以在Apifox中设置环境变量：

### 1. 创建环境

在Apifox中：
1. 点击左侧「环境管理」
2. 新建环境：`短链接本地测试`

### 2. 配置变量

| 变量名 | 变量值 | 说明 |
|--------|--------|------|
| admin_base_url | http://127.0.0.1:8902 | admin服务地址 |
| project_base_url | http://127.0.0.1:8001 | project服务地址 |
| test_id | 123456789 | 测试短链接ID（创建后填入） |
| test_full_url | nurl.ink/xxxxxx | 测试短链接（创建后填入） |
| test_gid_a | test-group-A | 测试分组A |
| test_gid_b | test-group-B | 测试分组B |

### 3. 使用变量

修改接口的Body为：
```json
{
  "id": {{test_id}},
  "originalGid": "{{test_gid_a}}",
  "gid": "{{test_gid_b}}",
  "fullShortUrl": "{{test_full_url}}",
  "originUrl": "https://www.updated-example.com",
  "describe": "使用环境变量测试"
}
```

---

## 📋 完整测试检查清单

### 测试前检查
- [ ] project 服务已启动（端口8001）
- [ ] admin 服务已启动（端口8902）
- [ ] MySQL 服务已启动
- [ ] Redis 服务已启动（如果使用）
- [ ] 已创建测试短链接并记录ID和fullShortUrl

### 场景一：同组修改
- [ ] 请求成功（返回code=0）
- [ ] 查询时能找到记录
- [ ] originUrl 已更新
- [ ] describe 已更新
- [ ] gid 保持不变
- [ ] 后台日志显示"gid没有变化，直接更新"

### 场景二：跨组移动
- [ ] 请求成功（返回code=0）
- [ ] 在新分组能查询到记录
- [ ] 在旧分组查询不到记录
- [ ] gid 已更新为新分组
- [ ] 后台日志显示"执行删除+插入操作"

### 错误场景
- [ ] 不传originalGid时返回错误提示
- [ ] originalGid错误时返回"记录不存在"

---

## 🐛 常见问题排查

### 问题1：返回"短链接记录不存在"

**可能原因**：
1. `originalGid` 填写错误
2. `fullShortUrl` 填写错误
3. 记录已被删除（del_flag=1）

**排查步骤**：
```bash
# 1. 检查日志
# 查看 project 服务控制台的日志输出

# 2. 直接查询数据库
SELECT * FROM t_link_0 WHERE full_short_url = 'nurl.ink/xxxxxx';
# 重复查询 t_link_1 到 t_link_15

# 3. 确认参数
# 重新执行查询接口获取最新的 id、gid、fullShortUrl
```

### 问题2：更新后查询不到数据

**可能原因**：
- 跨组移动时使用了旧的 gid 查询

**解决方法**：
```bash
# 使用新的 gid 查询
GET /api/short-link/admin/v1/page?gid=test-group-B&current=1&size=10
                                        ^^^^^^^^^^ 
                                        使用目标分组ID
```

### 问题3：数据出现重复

**可能原因**：
- 事务回滚失败
- 删除操作未成功

**排查步骤**：
```sql
-- 查询所有分片表中的重复记录
SELECT 
    't_link_0' as table_name, id, gid, full_short_url 
FROM t_link_0 WHERE full_short_url = 'nurl.ink/xxxxxx'
UNION ALL
SELECT 
    't_link_1' as table_name, id, gid, full_short_url 
FROM t_link_1 WHERE full_short_url = 'nurl.ink/xxxxxx'
-- ... 重复到 t_link_15
```

### 问题4：连接被拒绝

**检查清单**：
```bash
# 1. 检查 project 服务是否启动
curl http://127.0.0.1:8001

# 2. 检查 admin 配置
# admin/src/main/resources/application.yaml
short-link:
  project:
    url: http://127.0.0.1:8001  # 确认地址正确

# 3. 查看防火墙设置
```

---

## 📊 测试报告模板

测试完成后，可以按以下格式记录结果：

```
### 修改短链接接口测试报告

**测试时间**：2025-10-05
**测试人员**：[你的名字]
**测试环境**：本地开发环境

#### 测试数据
- 测试短链接ID：123456789
- 测试fullShortUrl：nurl.ink/xxxxxx
- 原始分组：test-group-A
- 目标分组：test-group-B

#### 测试结果

| 场景 | 预期结果 | 实际结果 | 状态 |
|------|----------|----------|------|
| 同组修改 | 记录更新成功 | ✅ 通过 | PASS |
| 跨组移动 | 旧组删除，新组插入 | ✅ 通过 | PASS |
| 缺少originalGid | 返回错误提示 | ✅ 通过 | PASS |
| originalGid错误 | 返回记录不存在 | ✅ 通过 | PASS |

#### 问题记录
无问题 / [记录发现的问题]

#### 结论
修改短链接接口功能正常，分库分表场景下能正确处理跨分片迁移。
```

---

## 🎯 下一步

测试通过后：
1. ✅ 提交代码到Git
2. ✅ 更新前端代码（传递originalGid字段）
3. ✅ 进行集成测试
4. ✅ 部署到测试环境

---

**祝测试顺利！如有问题，请查看后台日志或数据库记录进行排查。**




