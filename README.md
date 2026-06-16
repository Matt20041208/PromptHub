<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.2-blue" alt="Spring Cloud">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17">
  <img src="https://img.shields.io/badge/React-18-61dafb" alt="React 18">
  <img src="https://img.shields.io/badge/Python-3.8+-yellow" alt="Python">
  <img src="https://img.shields.io/badge/Redis-5.0-red" alt="Redis">
  <img src="https://img.shields.io/badge/RabbitMQ-3.8-orange" alt="RabbitMQ">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License">
</p>

# 🚀 PromptHub — AI 提示词交易市场

一个完整的微服务全栈项目，让你像交易商品一样买卖 AI 提示词。支持创作发布、搜索购买、评价收藏、AI 辅助优化、VIP 订阅会员。后端采用 Spring Cloud 微服务架构，集成 Redis 缓存/锁/计数器/黑名单/秒杀库存，RabbitMQ 异步消息与延时队列，Python Agent 接入 DeepSeek 大模型提供 AI 能力。

<p align="center">
  <img src="https://github.com/Matt20041208/PromptHub/blob/main/docs/architecture.png" alt="Architecture" width="800"/>
</p>

---

## 📖 目录

- [项目简介](#-项目简介)
- [系统架构](#-系统架构)
- [技术栈](#-技术栈)
- [功能清单](#-功能清单)
- [微服务模块](#-微服务模块)
- [Redis 深度应用](#-redis-深度应用)
- [RabbitMQ 消息驱动](#-rabbitmq-消息驱动)
- [快速开始](#-快速开始)
- [API 概览](#-api-概览)
- [项目亮点](#-项目亮点)

---

## 💡 项目简介

**PromptHub** 是一个 AI 提示词（Prompt）在线交易平台。用户可以像在电商平台买卖商品一样创作、发布、购买、评价提示词。

平台内置 **AI Agent**（基于 LangChain + DeepSeek），提供提示词优化、自动分类、质量评测、模板生成等智能工具，形成「**创作 → AI 辅助 → 发布 → 交易 → 评价通知**」完整闭环。

---

## 🏗 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                      React 18 + Vite                         │
│                    (Port 5173 / 8080)                        │
└──────────────────────────┬───────────────────────────────────┘
                           │ HTTP
┌──────────────────────────▼───────────────────────────────────┐
│              Spring Cloud Gateway :9000                       │
│         JWT Auth · Rate Limit · Route Dispatch               │
└────┬──────┬──────┬──────┬──────┬──────┬──────┬──────────────┘
     │      │      │      │      │      │      │
┌────▼──┐┌──▼───┐┌─▼────┐┌─▼────┐┌▼────┐┌▼────┐┌▼──────────┐
│ User  ││Prompt││Trade ││Review││Search││Notif││Python Agent│
│ :9101 ││:9102 ││:9103 ││:9104 ││:9105 ││:9106 ││:9107       │
│       ││      ││      ││      ││      ││      ││FastAPI     │
│       ││      ││      ││      ││(ES)  ││      ││LangChain   │
└──┬────┘└──┬───┘└──┬───┘└──┬───┘└──┬───┘└──┬───┘│DeepSeek    │
   │        │       │       │       │       │     └────────────┘
   │        │       │       │       │       │
┌──▼────────▼───────▼───────▼───────▼───────▼──────────────────┐
│              Middleware & Infrastructure                      │
│  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │  Nacos  │ │  Redis  │ │RabbitMQ  │ │    MySQL × 6     │  │
│  │ :8848   │ │ :6379   │ │ :5672    │ │ user/prompt/trade│  │
│  │ 注册配置│ │5种用法  │ │4条MQ链路 │ │review/search/ntfy│  │
│  └─────────┘ └─────────┘ └──────────┘ └──────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

**9 个微服务**，按业务领域垂直拆分，每个服务独立数据库、独立部署。

---

## 🛠 技术栈

### 后端 Java

| 分类 | 技术 | 版本 |
|------|------|------|
| 核心框架 | Spring Boot + Spring Cloud | 3.2.5 / 2023.0.2 |
| 服务治理 | Nacos（注册中心 + 配置中心）| 2.4.3 |
| API 网关 | Spring Cloud Gateway（WebFlux）| 2023.0.2 |
| ORM | MyBatis-Plus + Druid 连接池 | 3.5.7 / 1.2.23 |
| 数据库 | MySQL（6 个独立数据库）| 8.0 |
| 缓存 | Redis（Lettuce 客户端）| 5.0 |
| 消息队列 | RabbitMQ（Spring AMQP）| 3.8 |
| 搜索 | Elasticsearch | 7.17 |
| 安全 | JJWT（Token 鉴权）| 0.12.5 |
| 文档 | Knife4j（OpenAPI 3.0）| 4.5.0 |
| 工具 | Lombok + Hutool | 5.8.28 |

### AI 服务 Python

| 分类 | 技术 |
|------|------|
| Web 框架 | FastAPI + Uvicorn |
| AI 引擎 | LangChain + LangChain-OpenAI |
| LLM | DeepSeek V4（兼容 OpenAI API）|
| 任务队列 | Celery + Redis |

### 前端

| 分类 | 技术 |
|------|------|
| 框架 | React 18 + TypeScript |
| 构建 | Vite 5 |
| 路由 | React Router 6 |
| 渲染 | React Markdown |

---

## ✨ 功能清单

### 用户系统
- ✅ 注册 / 登录（JWT Token）
- ✅ 个人信息管理
- ✅ JWT 黑名单登出（Redis，TTL 自动过期）

### 提示词市场
- ✅ 分类树 + 标签筛选 + 关键词搜索 + 排序
- ✅ 提示词详情（热点缓存 5 分钟）
- ✅ **付费内容购买前截断显示**，购买后解锁
- ✅ 评价 + 收藏
- ✅ 浏览计数 Redis 缓冲 → 定时回写 MySQL

### 交易系统
- ✅ 下单 + 支付（Redis 分布式锁防并发）
- ✅ 余额 + 充值
- ✅ **MQ 延时队列：15 分钟未支付自动取消订单**
- ✅ **VIP 会员秒杀**：Redis 扣库存 + 锁 + MQ 通知

### AI 智能体
- ✅ 提示词优化（评分 + 改动说明）
- ✅ 自动分类 + 打标签
- ✅ 质量评测（四维度评分）
- ✅ 模板生成
- ✅ LLM 调试运行
- ✅ **发布页 AI 嵌入**：自动分类 / AI 优化
- ✅ **Agent 结果一键发布到市场**

### 通知系统
- ✅ 评价通知（MQ `review.created`）
- ✅ 支付通知（HTTP + MQ 双通道）
- ✅ VIP 购买通知（MQ `vip.bought`）
- ✅ 已读 / 全部已读

---

## 📦 微服务模块

| 模块 | 端口 | 职责 | 数据库 |
|------|------|------|--------|
| **prompt-gateway** | 9000 | API 网关、JWT 校验、前端代理 | - |
| **prompt-common** | - | 公共模块（DTO、工具类、异常处理）| - |
| **prompt-user** | 9101 | 用户注册登录、VIP 购买 | `prompt_user` |
| **prompt-prompt** | 9102 | 提示词 CRUD、分类、标签、版本 | `prompt_prompt` |
| **prompt-trade** | 9103 | 订单、支付、余额、充值 | `prompt_trade` |
| **prompt-review** | 9104 | 评价、收藏、评分 | `prompt_review` |
| **prompt-search** | 9105 | 全文搜索、热词 | `prompt_search` |
| **prompt-notify** | 9106 | 通知管理、MQ 消费 | `prompt_notify` |
| **prompt-agent** | 9107 | AI 智能体（Python/FastAPI）| - |
| **prompt-web** | 8080 | 前端（React/Vite）| - |

---

## 🔴 Redis 深度应用

| # | 场景 | 实现 | 关键技术 |
|---|------|------|---------|
| 1 | **热点缓存** | 提示词列表/详情 JSON 缓存，5-10min TTL，写操作清缓存 | `@Cacheable` + `GenericJackson2JsonRedisSerializer` |
| 2 | **浏览计数** | 每次浏览 `INCR`，60 秒定时 `GET` + `DELETE` 批量刷 MySQL | 避免高频 DB 写竞争 |
| 3 | **分布式锁** | 支付时 `SETNX` 锁订单号，5s 超时，value 匹配防误删 | `opsForValue().setIfAbsent` |
| 4 | **JWT 黑名单** | 登出时 `SET token TTL`，网关 `hasKey` 拦截，TTL = 剩余有效期 | 自动过期释放内存 |
| 5 | **秒杀库存** | VIP 限量 100 份，`DECR` 原子扣 + SETNX 锁防重复购买 | 比 DB 行锁快 100 倍 |

---

## 🐰 RabbitMQ 消息驱动

```
review 服务 ──JSON──→ review.exchange ──review.created──→ notify 服务（发评价通知）
                   └──prompt.rating───→ search 服务（更新 ES 评分）

trade 服务 ──JSON──→ trade.exchange ──vip.bought──────→ notify 服务（VIP 购买通知）
                   ├──order.delay─────→ [15min TTL] ──→ order.cancel.queue
                   │                                      └── 自动取消未支付订单
                   └──trade.order.paid（待恢复）
```

所有消息使用 `Jackson2JsonMessageConverter` 序列化，消费者用 Hutool JSON 解析。

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0
- Redis 5.0+
- RabbitMQ 3.8+
- Python 3.8+（仅 Agent）
- Node.js 18+（仅前端）

### 1. 初始化数据库

```bash
# 依次执行 SQL 文件创建 6 个数据库
mysql -u root -p < sql/prompt_user.sql
mysql -u root -p < sql/prompt_prompt.sql
mysql -u root -p < sql/prompt_trade.sql
mysql -u root -p < sql/prompt_review.sql
mysql -u root -p < sql/prompt_search.sql
mysql -u root -p < sql/prompt_notify.sql
```

### 2. 启动 Nacos

```bash
# 下载并启动 Nacos（单机模式）
cd nacos/bin
sh startup.sh -m standalone
```

### 3. 编译运行后端

```bash
mvn clean package -DskipTests

# 按顺序启动
java -jar prompt-gateway/target/prompt-gateway-1.0.0.jar &
java -jar prompt-user/target/prompt-user-1.0.0.jar &
java -jar prompt-prompt/target/prompt-prompt-1.0.0.jar &
java -jar prompt-trade/target/prompt-trade-1.0.0.jar &
java -jar prompt-review/target/prompt-review-1.0.0.jar &
java -jar prompt-search/target/prompt-search-1.0.0.jar &
java -jar prompt-notify/target/prompt-notify-1.0.0.jar &
```

### 4. 启动 AI Agent

```bash
cd prompt-agent
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

# 配置 API Key
cp .env.example .env
# 编辑 .env 填入 DeepSeek API Key

uvicorn app.main:app --host 0.0.0.0 --port 9107
```

### 5. 启动前端

```bash
cd prompt-web
npm install
npm run dev
```

访问 `http://localhost:5173` 即可使用。

---

## 📡 API 概览

所有接口通过网关统一接入，响应格式：

```json
{ "code": 200, "message": "success", "data": {...}, "timestamp": 1718534400000 }
```

### 用户服务 `/api/user`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/register` | 用户注册 | 否 |
| POST | `/login` | 用户登录 | 否 |
| GET | `/info` | 当前用户信息 | 是 |
| PUT | `/info` | 更新个人信息 | 是 |
| POST | `/logout` | 登出（加入黑名单）| 否 |
| POST | `/vip/buy` | 秒杀购买VIP | 是 |

### 提示词服务 `/api/prompt`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/` | 创建提示词 | 是 |
| GET | `/{id}` | 获取详情（VIP 自动解锁）| 是 |
| GET | `/list` | 分页列表（缓存）| 否 |
| GET | `/category` | 分类树 | 否 |
| GET | `/tag` | 标签列表 | 否 |

### 交易服务 `/api/trade`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/order` | 创建订单 | 是 |
| POST | `/order/{no}/pay` | 支付（分布式锁 + VIP 免付）| 是 |
| GET | `/order/list` | 订单列表 | 是 |
| GET | `/balance` | 查询余额 | 是 |
| POST | `/recharge` | 充值 | 是 |

### AI 智能体 `/api/agent`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/optimize` | 优化提示词 |
| POST | `/classify` | 自动分类 |
| POST | `/generate` | 生成模板 |
| POST | `/evaluate` | 质量评测 |
| POST | `/debug` | LLM 调试运行 |

> 完整 API 文档见 Knife4j: `http://localhost:9101/doc.html`（各服务）
> Python Agent: `http://localhost:9107/docs`（Swagger）

---

## 🌟 项目亮点

### 架构设计
- **9 个微服务 + 6 个独立数据库**，真正的微服务拆分
- **技术异构**：Java 做业务 + Python 做 AI，各取所长
- **消息驱动**：评价通知、VIP 通知、延时取消全部走 MQ
- **缓存分层**：热点数据 Redis 缓存，5 分钟内无 DB 查询

### Redis 多样化应用
- 缓存 / 计数器 / 分布式锁 / 黑名单 / 秒杀库存 —— 五种场景全覆盖

### 延时队列
- RabbitMQ 死信队列 + 15 分钟 TTL，自动取消超时订单，零定时任务，零轮询

### AI 全流程嵌入
- 发布时自动分类 + AI 优化 → 一键发布到市场 → 购买评价

### 安全
- JWT + Redis 黑名单双层保障
- VIP 自动解锁全部付费内容
- 付费内容购买前截断 + 渐变遮蔽

---

## 📄 License

MIT © 2024

---

<p align="center">
  <sub>Built with ❤️ by Matt</sub>
</p>
