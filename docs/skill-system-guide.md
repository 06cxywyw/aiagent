# Skill 系统使用指南

## 📋 功能说明

Skill 系统让你的 AI Agent 可以像我一样，支持插件化扩展功能。

### 核心功能

- ✅ **Skill 市场** - 搜索、浏览、下载 skills
- ✅ **本地管理** - 安装、卸载、更新 skills
- ✅ **执行引擎** - 运行不同类型的 skills
- ✅ **分类标签** - 按标签筛选 skills

---

## 🚀 快速开始

### 1. 搜索 Skill 市场

```bash
# 搜索所有 skills
curl "http://localhost:8123/api/skills/market/search"

# 按关键词搜索
curl "http://localhost:8123/api/skills/market/search?keyword=代码"

# 按标签搜索
curl "http://localhost:8123/api/skills/market/search?tag=数据分析"

# 分页
curl "http://localhost:8123/api/skills/market/search?page=1&pageSize=10"
```

**响应示例：**
```json
{
  "success": true,
  "count": 5,
  "page": 1,
  "pageSize": 20,
  "items": [
    {
      "id": "code-review",
      "name": "代码审查助手",
      "version": "1.0.0",
      "description": "自动审查代码质量、安全性和最佳实践",
      "author": "AI Agent Team",
      "tags": ["代码生成", "工具调用"],
      "downloads": 1250,
      "rating": 4.8,
      "ratingCount": 89,
      "installed": false,
      "downloadUrl": "https://cdn.aiagent-skills.com/code-review-1.0.0.zip"
    },
    {
      "id": "doc-summarizer",
      "name": "文档总结器",
      "version": "2.1.0",
      "description": "快速总结长文档，提取关键信息",
      "author": "Doc Team",
      "tags": ["文本处理", "总结"],
      "downloads": 3420,
      "rating": 4.9,
      "ratingCount": 156,
      "installed": false
    }
  ]
}
```

### 2. 获取热门 Skills

```bash
curl "http://localhost:8123/api/skills/market/popular?limit=10"
```

### 3. 查看 Skill 详情

```bash
curl "http://localhost:8123/api/skills/market/sql-generator"
```

**响应：**
```json
{
  "success": true,
  "item": {
    "id": "sql-generator",
    "name": "SQL 查询生成器",
    "version": "1.5.2",
    "description": "根据自然语言描述生成 SQL 查询",
    "readme": "# SQL 查询生成器\n\n支持多种数据库方言...",
    "author": "Data Team",
    "tags": ["数据分析", "代码生成"],
    "downloads": 2890,
    "rating": 4.7,
    "installed": true,
    "downloadUrl": "https://cdn.aiagent-skills.com/sql-generator-1.5.2.zip",
    "repositoryUrl": "https://github.com/aiagent/skill-sql-generator"
  }
}
```

### 4. 安装 Skill

```bash
curl -X POST http://localhost:8123/api/skills/install \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "doc-summarizer",
    "downloadUrl": "https://cdn.aiagent-skills.com/doc-summarizer-2.1.0.zip"
  }'
```

**响应：**
```json
{
  "success": true,
  "message": "Skill installed successfully"
}
```

### 5. 查看已安装的 Skills

```bash
curl "http://localhost:8123/api/skills/installed"
```

**响应：**
```json
{
  "success": true,
  "count": 3,
  "skills": [
    {
      "id": "doc-summarizer",
      "name": "文档总结器",
      "version": "2.1.0",
      "type": "PROMPT",
      "inputs": [...]
    }
  ]
}
```

### 6. 执行 Skill

```bash
curl -X POST http://localhost:8123/api/skills/doc-summarizer/execute \
  -H "Content-Type: application/json" \
  -d '{
    "document": "这是一篇很长的文档内容...",
    "length": "short",
    "focus": "关注技术要点"
  }'
```

**响应：**
```json
{
  "success": true,
  "output": "文档摘要：本文介绍了..."
}
```

### 7. 卸载 Skill

```bash
curl -X DELETE http://localhost:8123/api/skills/doc-summarizer
```

### 8. 更新 Skill

```bash
curl -X PUT http://localhost:8123/api/skills/doc-summarizer
```

### 9. 获取所有标签

```bash
curl "http://localhost:8123/api/skills/market/tags"
```

**响应：**
```json
{
  "success": true,
  "tags": [
    "数据分析",
    "文本处理",
    "图像处理",
    "代码生成",
    "翻译",
    "总结",
    "问答",
    "搜索",
    "工具调用"
  ]
}
```

---

## 📦 Skill 类型

### 1. PROMPT 类型

基于 Prompt 模板的 skill，最简单常用。

**示例：文档总结器**
```json
{
  "type": "PROMPT",
  "promptTemplate": "请总结以下文档...\n{document}"
}
```

### 2. TOOL 类型

调用外部工具的 skill。

**示例：图片分析**
```json
{
  "type": "TOOL",
  "tools": ["image-recognition", "ocr"]
}
```

### 3. CODE 类型

自定义 Java 代码的 skill。

**示例：数据处理**
```json
{
  "type": "CODE",
  "handlerClass": "com.example.skills.DataProcessor"
}
```

### 4. COMPOSITE 类型

组合多个 skills 的复杂 skill。

**示例：完整报告生成**
```json
{
  "type": "COMPOSITE",
  "dependencies": ["data-analyzer", "chart-generator", "doc-formatter"]
}
```

---

## 🛠️ 创建自己的 Skill

### 1. 创建 Skill 目录

```bash
mkdir -p ~/.aiagent/skills/my-skill
cd ~/.aiagent/skills/my-skill
```

### 2. 创建 skill.json

```json
{
  "id": "my-skill",
  "name": "我的 Skill",
  "version": "1.0.0",
  "description": "这是我的第一个 skill",
  "author": "Your Name",
  "type": "PROMPT",
  "tags": ["自定义"],
  "inputs": [
    {
      "name": "input",
      "type": "string",
      "description": "输入内容",
      "required": true
    }
  ],
  "output": {
    "type": "string",
    "description": "输出结果"
  },
  "promptTemplate": "处理以下内容：{input}"
}
```

### 3. 重新加载 Skills

```bash
# 重启应用，或调用重新加载接口
curl -X POST http://localhost:8123/api/skills/reload
```

---

## 📂 Skill 目录结构

```
~/.aiagent/skills/
├── doc-summarizer/
│   ├── skill.json          # Skill 配置
│   ├── README.md           # 说明文档
│   └── icon.png            # 图标（可选）
├── sql-generator/
│   ├── skill.json
│   └── examples/           # 示例（可选）
│       └── example1.json
└── code-review/
    ├── skill.json
    └── handler.jar         # CODE 类型需要
```

---

## 🎯 Skill 配置详解

### 完整配置示例

```json
{
  "id": "skill-id",                    // 唯一标识
  "name": "Skill 名称",                // 显示名称
  "version": "1.0.0",                  // 版本号
  "description": "简短描述",           // 一句话描述
  "author": "作者名",                  // 作者
  "type": "PROMPT",                    // 类型
  "tags": ["标签1", "标签2"],         // 标签

  "inputs": [                          // 输入参数
    {
      "name": "param1",
      "type": "string",
      "description": "参数说明",
      "required": true,
      "defaultValue": "默认值"
    }
  ],

  "output": {                          // 输出定义
    "type": "string",
    "description": "输出说明"
  },

  "promptTemplate": "Prompt 模板 {param1}",

  "tools": ["tool1", "tool2"],         // TOOL 类型
  "handlerClass": "com.example.Handler", // CODE 类型
  "dependencies": ["skill1", "skill2"],  // COMPOSITE 类型

  "metadata": {                        // 元数据
    "category": "分类",
    "license": "MIT",
    "homepage": "https://..."
  }
}
```

---

## 🔧 高级功能

### 1. 按标签筛选

```bash
curl "http://localhost:8123/api/skills/market/search?tag=数据分析"
```

### 2. 组合搜索

```bash
curl "http://localhost:8123/api/skills/market/search?keyword=SQL&tag=数据分析"
```

### 3. 获取统计信息

```bash
curl "http://localhost:8123/api/skills/stats"
```

**响应：**
```json
{
  "success": true,
  "stats": {
    "total": 15,
    "byType": {
      "PROMPT": 10,
      "TOOL": 3,
      "CODE": 2
    }
  }
}
```

---

## 📊 Skill 市场数据（模拟）

当前提供 5 个示例 skills：

| Skill | 类型 | 下载量 | 评分 | 状态 |
|-------|------|--------|------|------|
| 代码审查助手 | PROMPT | 1,250 | 4.8 | 未安装 |
| 文档总结器 | PROMPT | 3,420 | 4.9 | 未安装 |
| SQL 生成器 | PROMPT | 2,890 | 4.7 | 已安装 |
| 翻译助手 | PROMPT | 5,670 | 4.9 | 未安装 |
| 图片分析 | TOOL | 1,890 | 4.6 | 未安装 |

---

## ⚠️ 注意事项

### 1. 当前限制

- **下载功能未实现** - `installSkill()` 是占位符
- **执行引擎简化** - 只支持基础的 Prompt 替换
- **市场 API 是模拟数据** - 需要对接真实的 Skill 市场

### 2. 需要实现的功能

**安装逻辑：**
```java
public void installSkill(String skillId, String downloadUrl) {
    // 1. 下载 skill 包
    byte[] data = restTemplate.getForObject(downloadUrl, byte[].class);

    // 2. 解压到 skills 目录
    String targetDir = skillsDirectory + "/" + skillId;
    unzip(data, targetDir);

    // 3. 验证 skill.json
    SkillDefinition skill = loadSkill(new File(targetDir));

    // 4. 加载到内存
    loadedSkills.put(skill.getId(), skill);
}
```

**执行引擎：**
```java
private String executePromptSkill(SkillDefinition skill, Map<String, Object> inputs) {
    // 替换变量
    String prompt = skill.getPromptTemplate();
    for (Map.Entry<String, Object> entry : inputs.entrySet()) {
        prompt = prompt.replace("{" + entry.getKey() + "}", entry.getValue().toString());
    }

    // 调用 LLM
    ChatResponse response = chatModel.call(prompt);
    return response.getResult().getOutput().getText();
}
```

### 3. 生产环境建议

- 使用真实的 Skill 市场 API
- 添加 Skill 签名验证（防止恶意代码）
- 实现沙箱隔离（CODE 类型的 skill）
- 添加版本管理和依赖解析
- 支持 Skill 热更新

---

## 🎯 与 Claude Code 的对比

| 功能 | Claude Code | 你的实现 |
|------|-------------|----------|
| Skill 市场 | ✅ 官方市场 | ✅ 自定义市场 |
| 搜索功能 | ✅ 关键词+标签 | ✅ 关键词+标签 |
| 安装管理 | ✅ 自动安装 | 🚧 待实现 |
| 执行引擎 | ✅ 完整支持 | 🚧 基础支持 |
| 类型支持 | ✅ 多种类型 | ✅ 4种类型 |

---

## 📝 TODO

1. **实现下载和安装逻辑**
   - HTTP 下载
   - ZIP 解压
   - 文件验证

2. **完善执行引擎**
   - 集成 LLM 调用
   - 工具调用支持
   - 错误处理

3. **对接真实市场**
   - 搭建 Skill 市场后端
   - 实现上传/发布功能
   - 添加评分和评论

4. **安全加固**
   - Skill 签名验证
   - 沙箱隔离
   - 权限控制

---

## 🤝 需要帮助？

查看示例 skills：
- `skills-examples/doc-summarizer/skill.json`
- `skills-examples/sql-generator/skill.json`
- `skills-examples/code-review/skill.json`

测试接口：
```bash
# 搜索市场
curl "http://localhost:8123/api/skills/market/search?keyword=代码"

# 查看已安装
curl "http://localhost:8123/api/skills/installed"
```
