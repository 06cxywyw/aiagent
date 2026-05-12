# 记忆清理功能使用指南

## 📋 功能说明

自动清理低价值的长期记忆，防止数据库无限增长。

### 清理规则

| 重要性得分 | 未访问天数 | 操作 |
|-----------|----------|------|
| < 0.3 | 30天 | 删除 |
| < 0.5 | 90天 | 删除 |
| < 0.7 | 180天 | 删除 |
| 任意 | 365天 | 删除 |

### 重要性评分规则

**基础分：0.5**

加分项：
- 长度 > 100字符：+0.05
- 长度 > 200字符：+0.05
- 长度 > 500字符：+0.1
- 包含关键词（重要、记住、必须等）：+0.1
- 包含手机号：+0.05
- 包含日期：+0.05
- 包含邮箱：+0.05
- 包含数字：+0.05

减分项：
- 包含问号：-0.05

**最终得分范围：0.0 - 1.0**

---

## 🚀 快速开始

### 1. 配置文件

在 `application-memory.yml` 中配置：

```yaml
memory:
  cleanup:
    enabled: true          # 启用自动清理
    dry-run: false         # false=真实删除，true=只记录不删除
    cron: "0 0 2 * * ?"   # 每天凌晨2点执行
```

### 2. 启动应用

```bash
# 应用会自动加载 memory 配置
mvn spring-boot:run
```

### 3. 查看日志

```
2026-05-10 02:00:00 INFO  MemoryCleanupService - ========== 开始记忆清理任务 ==========
2026-05-10 02:00:01 INFO  MemoryCleanupService - 执行清理规则: 低重要性30天未访问 (importance < 0.3, inactive > 30 days)
2026-05-10 02:00:01 INFO  MemoryCleanupService - 规则 低重要性30天未访问 删除了 15 条记忆
2026-05-10 02:00:02 INFO  MemoryCleanupService - 执行清理规则: 中重要性90天未访问 (importance < 0.5, inactive > 90 days)
2026-05-10 02:00:02 INFO  MemoryCleanupService - 规则 中重要性90天未访问 删除了 8 条记忆
...
2026-05-10 02:00:05 INFO  MemoryCleanupService - ========== 记忆清理完成 ==========
2026-05-10 02:00:05 INFO  MemoryCleanupService - 总计删除: 35 条记忆
2026-05-10 02:00:05 INFO  MemoryCleanupService - 耗时: 5234 ms
```

---

## 🧪 测试接口

### 1. 添加记忆

```bash
curl -X POST http://localhost:8123/api/memory/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "content": "我叫张三，住在北京，手机号13812345678，一定要记住我的生日是1990-01-01"
  }'
```

**响应：**
```json
{
  "success": true,
  "message": "记忆已添加"
}
```

**日志：**
```
INFO  LongTermMemory - 长期记忆已存储 (重要性: 0.85): 我叫张三，住在北京，手机号13812345...
```

### 2. 检索记忆

```bash
curl "http://localhost:8123/api/memory/search?userId=user123&query=张三的信息&limit=5"
```

**响应：**
```json
{
  "success": true,
  "count": 2,
  "memories": [
    "我叫张三，住在北京，手机号13812345678...",
    "张三喜欢打篮球，每周末都会去体育馆..."
  ]
}
```

### 3. 获取实体信息

```bash
curl "http://localhost:8123/api/memory/entities/user123"
```

**响应：**
```json
{
  "success": true,
  "userId": "user123",
  "entities": {
    "name": "张三",
    "phone": "13812345678",
    "location": "北京",
    "date": "1990-01-01"
  }
}
```

### 4. 手动触发清理（测试用）

```bash
curl -X POST http://localhost:8123/api/memory/cleanup
```

**响应：**
```json
{
  "success": true,
  "message": "记忆清理任务已触发"
}
```

### 5. 清空用户记忆

```bash
curl -X DELETE http://localhost:8123/api/memory/clear/user123
```

---

## 🔧 高级配置

### 试运行模式（推荐先测试）

```yaml
memory:
  cleanup:
    dry-run: true  # 只记录不删除
```

**日志：**
```
INFO  MemoryCleanupService - [DRY RUN] 规则 低重要性30天未访问 将删除 15 条记忆（实际未删除）
```

### 自定义清理时间

```yaml
memory:
  cleanup:
    cron: "0 0 3 * * ?"  # 每天凌晨3点
    # 或
    cron: "0 0 2 * * 0"  # 每周日凌晨2点
```

### 禁用自动清理

```yaml
memory:
  cleanup:
    enabled: false
```

---

## 📊 重要性得分示例

| 内容 | 得分 | 说明 |
|------|------|------|
| "你好" | 0.0 | 太短，不存储 |
| "我喜欢打篮球，每周末都会去体育馆锻炼身体" | 0.6 | 基础分0.5 + 长度0.1 |
| "重要：我的手机号是13812345678，一定要记住" | 0.85 | 基础分0.5 + 长度0.15 + 关键词0.1 + 手机号0.05 + 数字0.05 |
| "你觉得呢？" | 0.45 | 基础分0.5 - 问号0.05 |
| "我的生日是1990-01-01，邮箱test@qq.com" | 0.75 | 基础分0.5 + 长度0.1 + 日期0.05 + 邮箱0.05 + 数字0.05 |

---

## ⚠️ 注意事项

### 1. VectorStore 限制

当前 Spring AI 的 VectorStore 接口**不支持按 metadata 条件删除**。

`MemoryCleanupService` 中的删除逻辑是**伪代码**，需要根据你使用的 VectorStore 实现调整：

**PgVector 实现示例：**
```java
// 使用 JdbcTemplate 直接操作数据库
jdbcTemplate.update("""
    DELETE FROM vector_store
    WHERE metadata->>'importance' < ?
      AND metadata->>'last_access' < ?
    """,
    maxImportance,
    cutoffTime.toString()
);
```

### 2. 访问信息更新

`updateAccessInfo()` 方法目前只更新内存中的对象，**不会持久化到数据库**。

需要实现持久化逻辑：
```java
// 方案1：删除后重新添加
vectorStore.delete(List.of(doc.getId()));
vectorStore.add(List.of(doc));

// 方案2：使用支持更新的 VectorStore
vectorStore.update(doc);
```

### 3. 性能考虑

- 清理任务在凌晨执行，避免影响白天业务
- 如果记忆数量超过 10 万条，建议分批处理
- 可以添加进度日志，方便监控

---

## 🎯 效果预估

假设你的系统：
- 每天新增 1000 条记忆
- 平均重要性得分 0.5

**清理效果：**
- 30天后：删除约 30% 低价值记忆（300条）
- 90天后：删除约 50% 中价值记忆（500条）
- 180天后：删除约 70% 高价值记忆（700条）
- 1年后：删除所有未访问记忆

**稳定状态：**
- 数据库保持在 **3-6 个月**的活跃记忆
- 存储空间节省 **60-80%**

---

## 📝 TODO

1. **实现 VectorStore 的条件删除**
   - 根据你使用的 VectorStore（PgVector/Chroma/Pinecone）实现

2. **实现访问信息持久化**
   - 每次检索后更新 `last_access` 和 `access_count`

3. **添加统计接口**
   - 总记忆数量
   - 各重要性等级分布
   - 存储空间占用

4. **添加监控告警**
   - 清理失败告警
   - 存储空间告警
   - 清理数量异常告警

---

## 🤝 需要帮助？

如果遇到问题，检查：
1. 日志中是否有错误信息
2. `dry-run` 是否设置为 `true`（测试模式）
3. VectorStore 是否支持你需要的操作
4. 定时任务是否正常触发（检查 `@EnableScheduling`）
