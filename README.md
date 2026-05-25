# AI Daily — 飞书 AI 日报 MVP

第一阶段目标：**稳定接收飞书群聊 / 私聊消息并入库**，区分 `GROUP` 与 `PRIVATE`。

## 技术栈

- Java 8
- Spring Boot 2.7
- MySQL 5.7+ / 8.0 + Flyway
- 飞书事件订阅 `im.message.receive_v1`

## 环境要求

- JDK 8（`java -version` 显示 1.8）
- Maven 3.6+
- 本机已安装并启动的 MySQL（不使用 Docker）

## 快速启动

### 1. 创建数据库

在 MySQL 客户端执行：

```sql
CREATE DATABASE IF NOT EXISTS ai_daily
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env`，填入数据库与飞书凭证：

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=ai_daily
export DB_USER=root
export DB_PASSWORD=你的密码
export FEISHU_APP_ID=cli_xxxxxxxx
export FEISHU_APP_SECRET=xxxxxxxx
export FEISHU_VERIFICATION_TOKEN=xxxxxxxx
export FEISHU_ENCRYPT_KEY=
```

或在 IDE 运行配置里写入上述变量。

### 3. 运行应用

```bash
mvn spring-boot:run
```

健康检查：`GET http://localhost:8080/health`

## 飞书开放平台配置

1. **创建企业自建应用**，记录 App ID、App Secret。
2. **权限**（至少）：
   - 获取与发送单聊、群组消息
   - 读取用户发给机器人的单聊消息（私聊）
   - 接收群聊中 @机器人 的消息，或配置「接收群聊所有消息」（按企业合规选择）
3. **事件订阅** → 添加事件 `im.message.receive_v1`。
4. **请求 URL**：`https://<你的公网域名>/feishu/event`  
   本地开发可用 [ngrok](https://ngrok.com) / Cloudflare Tunnel 暴露 `8080`。
5. **Verification Token**：与 `FEISHU_VERIFICATION_TOKEN` 一致。
6. **Encrypt Key**：若开启加密则填写 `FEISHU_ENCRYPT_KEY`；未开启则留空。  
   **重要**：开启加密后，URL 验证时不能返回明文 `challenge`，必须返回 `{"encrypt":"..."}`（代码已处理）。
7. 将**机器人拉入需要采集的群**；私聊需用户主动给机器人发消息。

## 核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/feishu/event` | 飞书事件回调（URL 验证 + 消息接收） |
| GET | `/health` | 健康检查 |

## 数据表 `message_raw`

| 字段 | 说明 |
|------|------|
| `chat_type` | `GROUP`（含群聊、话题 thread）/ `PRIVATE`（p2p 单聊） |
| `chat_id` | 飞书会话 ID |
| `content` | 文本消息正文；其他类型带类型前缀 |
| `raw_payload` | 完整事件 JSON，便于后续 AI 重跑 |

## 聊天类型映射

| 飞书 `chat_type` | 入库 `chat_type` |
|------------------|------------------|
| `group` | GROUP |
| `topic` | GROUP |
| `p2p` | PRIVATE |

## 验证消息是否入库

```sql
SELECT id, chat_type, chat_id, sender_open_id, content, send_time
FROM message_raw
ORDER BY send_time DESC
LIMIT 20;
```

## 阶段二：AI 分析 → work_event（已实现）

消息入库后 **异步** 调用 DeepSeek，提取工作事件写入 `work_event`。

### 配置大模型（`ai.provider`）

**DeepSeek（云端）**

```yaml
ai:
  provider: deepseek
  deepseek:
    api-key: sk-你的密钥
    base-url: https://api.deepseek.com
    model: deepseek-chat
```

**Ollama（本地，如 qwen2.5:7b）**

先确保本机已拉取并运行模型：

```bash
ollama pull qwen2.5:7b
ollama serve   # 默认 http://localhost:11434
```

```yaml
ai:
  provider: ollama
  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:7b
    timeout-ms: 120000
```

Ollama 使用 OpenAI 兼容接口 `/v1/chat/completions`，无需 API Key（走代理时可配 `api-key`）。

### 工作事件表

（表结构请自行在 MySQL 手动维护。）

| 字段 | 说明 |
|------|------|
| `message_raw_id` | 来源消息 |
| `project_name` | 项目名 |
| `event_type` | TASK_DONE / BLOCKER / … |
| `summary` | 事件摘要 |
| `status` | DONE / IN_PROGRESS / BLOCKED / OPEN |
| `event_time` | 事件时间（默认取消息发送时间） |

### 调试接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/analyze/{messageRawId}` | 手动对某条消息重新分析 |
| GET | `/admin/work-events` | 查看全部工作事件 |
| GET | `/admin/work-events/by-message/{id}` | 按消息查看事件 |

### 扩展其他大模型

实现 `LlmClient` 接口，或复用 `OpenAiCompatibleLlmClient`（OpenAI 兼容 API），在 `LlmConfig` 注册 Bean 并设置 `ai.provider`。

## 阶段三：AI 生成日报 daily_report（已实现）

### 手动建表

执行 [docs/sql/daily_report.sql](src/main/resources/db/migration/daily_report.sql)（不使用 Flyway）。

### 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/daily-reports/generate?date=2026-05-21` | 查当日 `message_raw`（按时间排序）→ **一次 AI 调用**生成日报 → 入库 |
| GET | `/api/daily-reports?date=2026-05-21` | 查询已生成的日报 |

同一天重复 POST 会**覆盖**该日 `content`。

### 数据来源

按 `report_date`（Asia/Shanghai）筛选 `message_raw.send_time`，升序拼入 prompt。  
生成前会过滤明显噪声（收到、好的、哈哈等），**不依赖** `work_event`，避免每条消息单独调 API。

### 示例

```bash
curl -X POST "http://localhost:8080/api/daily-reports/generate?date=2026-05-21"
```

## 下一阶段（未实现）

- 飞书机器人推送日报
