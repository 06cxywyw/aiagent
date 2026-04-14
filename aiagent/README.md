# AI Agent System（智能体系统）

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.5-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Redis](https://img.shields.io/badge/Redis-7-red)
![AI](https://img.shields.io/badge/AI-Agent-purple)
![License](https://img.shields.io/badge/license-MIT-brightgreen)

---

## 📋 项目简介

**AI Agent System（智能体系统）** 是一个基于 **Spring Boot + Spring AI** 构建的智能体服务平台。

在传统后端系统架构基础上，引入大模型能力，实现：

- 🤖 多轮对话（上下文记忆）
- 🧠 对话记忆持久化
- 🔍 语义检索（RAG）
- 🧰 工具调用（Tool Calling）
- 🌐 MCP 协议扩展能力
- 🧩 CoT 推理优化

实现从 **传统业务系统 → AI智能体系统** 的升级。

---

## 🏗️ 系统架构

<pre>
Client（前端 / 调用方）
        │
        ▼
   Spring Boot（AI Agent）
        │
        ├── Spring AI（大模型调用）
        ├── Redis（缓存 / 对话记忆）
        ├── PostgreSQL + PGvector（向量数据库）
        └── MCP Server（工具扩展）
</pre>

---

## ⭐ 项目亮点

### 1. AI Agent 架构设计
基于 Spring AI 构建统一 AI 调用入口，支持多轮对话与上下文管理，实现可扩展智能体能力体系。

### 2. RAG（检索增强生成）
自定义 VectorStore，结合 EmbeddingModel 进行语义向量化，基于 PGvector 实现相似度检索，支持 TopK 检索与多条件过滤。

### 3. Tool Calling 工具调用体系
基于注解实现工具注册与调用，支持 AI 自动选择工具执行，集成文件操作、网络搜索、网页抓取、命令执行及 PDF 生成等能力。

### 4. MCP 协议扩展能力
基于 MCP Server 封装外部服务，集成 Pexels 图片搜索 API，实现 AI 联网图片检索能力，并支持 Stdio 与 SSE 双通信模式。

### 5. 对话记忆系统
基于 Kryo 实现文件持久化，并支持 Redis 扩展，实现上下文恢复与过期控制。

### 6. CoT 推理优化
引入 Chain of Thought 推理机制，提升复杂问题处理能力与结果可解释性。

---

## 🏗️ 项目结构


ai-agent/
├── common/ # 通用模块
├── model/ # 数据对象（DTO、Entity、VO）
├── controller/ # 接口层
├── service/ # 业务逻辑层
├── config/ # 配置类
├── tool/ # AI工具定义
├── memory/ # 对话记忆实现（File / Redis）
└── pom.xml


---

## 🤖 工作职责

### 对话记忆持久化
自主实现基于文件系统的 ChatMemory，结合 Kryo 高性能序列化库持久化对话历史数据，解决服务重启后上下文丢失问题，同时支持 Redis 扩展。

### 前端协同开发
使用 Claude Code 基于后端接口规范生成前端基础页面结构，并完成业务逻辑开发与样式优化。

### 结构化输出
基于 Spring AI 实现结构化响应，将 AI 输出转换为 Java 对象，方便后续处理与展示。

### 向量检索（RAG）
自定义 VectorStore，结合 EmbeddingModel 将文本转化为向量并存储至 PGvector，实现语义检索与多条件过滤。

### 工具调用体系
基于 Spring AI Tool Calling 机制，支持文件操作、网络搜索、网页抓取、命令执行、PDF 生成等能力扩展。

### MCP 服务集成
集成 Pexels API 实现图片搜索能力，支持 Stdio 与 SSE 两种通信模式。

### 推理优化（CoT）
引入 Chain of Thought 推理策略，提升复杂问题处理能力与结果准确性。

---

## 🛠️ 技术栈

| 技术 | 用途 |
|------|------|
| Spring Boot | 后端框架 |
| Spring AI | AI能力集成 |
| Redis | 缓存 / 对话记忆 |
| PostgreSQL | 数据存储 |
| PGvector | 向量检索 |
| Kryo | 高性能序列化 |
| MCP Server | 工具扩展 |

---

## 🚀 快速开始

### 环境要求

- Java 21+
- Maven 3.6+
- Redis
- PostgreSQL（需安装 PGvector）

---

### 编译项目


mvn clean install


---

### 启动项目


mvn spring-boot:run


---

## ⚙️ 配置说明


spring:
datasource:
url: jdbc:postgresql://localhost:5432/ai_agent
username: postgres
password: your_password

data:
redis:
host: localhost
port: 6379

ai:
dashscope:
api-key: your_api_key


---

## 🐛 故障排查

### Redis 连接失败

- 检查 Redis 是否启动
- 检查 IP / 端口 / 密码

### AI 功能不可用

- 检查 API Key
- 确认网络连接

---

## 👥 贡献（Contribution）

欢迎任何形式的贡献：

- 提交 Issue 反馈问题
- 提交 Pull Request 优化代码
- 提出新的功能建议

如果这个项目对你有帮助，欢迎点个 ⭐ 支持一下！

---

## 📄 License

MIT License