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

## 下一阶段（未实现）

- AI 分析 → `work_event`
- 定时任务 → `daily_report`
- 飞书机器人推送日报
