# Dify 本机部署与学习空间对接说明

## 一、本机一键部署

学习空间已占用 **80**（前端）、**8080**（后端）、**3307**（MySQL），Dify 默认使用 **8090**。

```bash
cd "/Users/zhangyouyu/Desktop/信息科技学习空间"
bash scripts/setup-dify.sh
```

浏览器打开：**http://localhost:8090/install**  
按向导创建管理员账号。

### 电脑重启后如何启动（必读）

Dify 与学习空间是 **两套独立的 Docker Compose**，只启动学习空间 **不会** 自动启动 Dify。

**正确顺序：**

1. 先打开 **Docker Desktop**，等左下角/菜单栏显示 **Running**（未启动时所有 `docker` 命令都会失败）。
2. 启动 Dify（端口 **8090**）：

```bash
cd "/Users/zhangyouyu/Desktop/信息科技学习空间/deploy/dify/docker"
docker compose up -d
```

3. 启动学习空间（端口 **80** / **8080**）：

```bash
cd "/Users/zhangyouyu/Desktop/信息科技学习空间"
docker compose up -d
```

4. 浏览器访问 Dify：**http://localhost:8090**（不是 `http://localhost`，后者是学习空间前端）。

**检查是否在跑：**

```bash
cd deploy/dify/docker && docker compose ps
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/apps
```

`docker compose ps` 里 `nginx`、`api`、`web` 应为 **Up**；`curl` 返回 **200** 表示正常。

| 现象 | 原因 |
|------|------|
| 浏览器打不开 8090 | Docker Desktop 未启动，或没在 `deploy/dify/docker` 下执行过 `docker compose up -d` |
| 只开了 `localhost` | 学习空间在 80 端口，Dify 在 **8090** |
| `permission denied` / `Cannot connect to docker` | 先启动 Docker Desktop |

### 常见问题

| 现象 | 处理 |
|------|------|
| `docker: command not found` | 安装并启动 Docker Desktop |
| 8090 打不开 | `cd deploy/dify/docker && docker compose ps` 看 nginx/api 是否 healthy |
| 端口被占用 | `DIFY_PORT=8091 bash scripts/setup-dify.sh` |
| 克隆很慢 | 配置 git 代理或使用 Gitee 镜像手动放到 `deploy/dify` |
| 无法安装模型供应商 / 插件安装失败 | 见下方「模型供应商安装失败」 |

### 模型供应商安装失败（常见）

Dify 1.14+ 的模型供应商以**插件**形式安装，首次会从 PyPI 拉取 Python 依赖。国内直连 `pypi.org` 很慢时，日志会出现：

`init process exited due to no activity for 120 seconds` / `failed to init environment`

**处理：** 编辑 `deploy/dify/docker/.env`：

```bash
PIP_MIRROR_URL=https://pypi.tuna.tsinghua.edu.cn/simple
PLUGIN_PYTHON_ENV_INIT_TIMEOUT=600
```

然后重启插件服务并重新在控制台安装供应商：

```bash
cd deploy/dify/docker
docker compose up -d plugin_daemon
```

在 Dify → **设置 → 模型供应商** 中再次点击安装（DeepSeek、硅基流动等），等待 3～10 分钟。

---

## 二、与学习空间的关系（架构）

```
教师/学生浏览器
    → 学习空间 Vue (80) + Spring Boot (8080)
    → 调用 /api/ai/*（后续开发）
    → Dify API (8090) + 知识库
```

- API Key **只保存在后端**，不暴露给浏览器。
- 学生提交数据在 `learn_submission`，评价时由后端整理后发给 Dify。

---

## 三、云服务器部署思路（可行）

推荐结构：

| 组件 | 建议 |
|------|------|
| 学习空间 | Docker Compose 原样部署；域名 `learn.yourdomain.com` |
| Dify | 同机另一 Compose 或独立子域 `dify.yourdomain.com` |
| 数据库 | 生产用云 RDS；Dify 自带 Postgres 可独立 volume |
| 素材库 | 先在 Dify 知识库上传；后续可增加 `private_files/knowledge/` 同步脚本 |

环境变量示例（学习空间后端 `application-prod.yml`）：

```yaml
dify:
  base-url: https://dify.yourdomain.com
  api-key: app-xxxxxxxx
  recommend-app-id: # 活动推荐应用 ID（在 Dify 创建后填写）
  evaluate-app-id:  # 学情评价应用 ID
```

云上与本地差异仅 **URL 和密钥**，代码无需两套。

---

## 四、下一步：在 Dify 里创建什么

1. **知识库**：上传信息科技教案、课标 PDF、探究单说明（可从 `private_files` 整理）
2. **应用 · 活动推荐**（Chatflow/Workflow）：输入课目标 → 输出 JSON 活动列表
3. **应用 · 学情评价**：输入学生提交摘要 → 输出分数、SOLO、等级、评语
4. 在应用「API 访问」中复制 **API Key** 与 **API Base URL**，交给学习空间后端配置

学习空间侧 P1 开发将对接上述两个应用。

### 活动推荐 API Key 配置

在 Dify【活动推荐】→ **访问 API** 复制 Key 后，任选一种方式配置：

```bash
# Docker 部署（推荐）：项目根目录创建 .env 或 export
export DIFY_RECOMMEND_API_KEY=app-你的密钥
cd 信息科技学习空间
docker compose up -d --build backend frontend
```

或在 `backend/src/main/resources/application.yml` 中填写 `dify.recommend-api-key`（勿提交到 Git）。

教师端 **活动编辑** 页：选择教材与课时 → **AI 推荐活动** → 勾选 → **采纳所选**。

**应用类型与 API 路由：**

| Dify 应用类型 | 配置 `recommend-app-mode` | API |
|---------------|---------------------------|-----|
| **工作流（Workflow）** | `workflow`（当前默认） | `POST /v1/workflows/run` |
| Chatflow / 对话流 | `chat` | `POST /v1/chat-messages` |
| 文本生成（Completion） | `completion` | `POST /v1/completion-messages` |

### Workflow 活动推荐（推荐）

Chatflow 会强制带 `userinput.query`，且变量易未传入 LLM。**建议用 Workflow 应用**：

1. Dify 新建应用 → 类型选 **工作流（Workflow）**，名称如「活动推荐」
2. **开始** 节点输入字段（与学习空间 API 一致，仅 2 项）：

| 变量名 | 类型 | 示例 |
|--------|------|------|
| `grade_name` | 文本 | 五年级下册 |
| `lesson_title` | 文本 | 第一单元 生活中的系统 第1课 身边的系统 |

> 学习空间会把 **单元名 + 课时标题** 拼进 `lesson_title`，便于在六册 PDF 知识库中检索。

3. **推荐工作流结构（与教师端三板块一致）**

教师端「活动编辑」只需 AI 生成 **文字草案**，教师修改确认后，再在平台点「生成活动」调用【互动页生成】。  
因此【活动推荐】工作流 **不要** 再生成 HTML、不要「生成活动五下」等第二个 LLM，建议简化为：

```text
开始 → 知识检索（本册教材知识库）→ LLM（课堂草案）→ 结束
```

- **知识检索** 查询：`{{grade_name}} {{lesson_title}} 学习目标 学习活动 学习评价`
- **LLM** 必须引用：`grade_name`、`lesson_title`、`知识检索.result`
- **结束** 节点：输出变量名 **`text`**，值为 `{{LLM.text}}`

4. **LLM 系统提示词（可直接粘贴）**

```text
你是小学信息科技备课助手。根据教材检索结果，为指定课时撰写「课堂学习草案」，供教师修改后使用。

输入：
- 册次：{{grade_name}}
- 课时：{{lesson_title}}
- 教材片段：{{知识检索.result}}

要求：
1. 只输出 Markdown，不要 HTML，不要代码块包裹全文。
2. 必须且仅包含以下三个二级标题（顺序固定），标题文字必须完全一致：
   ## 学习目标
   ## 学习活动
   ## 学习评价

3. 「学习目标」：以学生为主语（如「我能…」），2～3 条，说明学什么、学到什么程度算合格。不要写教师教学重点。

4. 「学习活动」：设计 2～3 个课堂活动，每个活动用三级标题：
   ### 活动1：标题
   ### 活动2：标题
   （如有活动3同样格式）
   每个活动下写：活动目的、简要步骤、建议时长（约 8～15 分钟）。只写文字描述，不要生成网页或代码。

5. 「学习评价」：写本课学习评价标准（可含自评要点、合格标准），与学生目标呼应。

6. 内容须符合 {{grade_name}} 对应教材，勿编造其他册次知识点。
```

#### 可选：LLM「结构化输出」（JSON Schema）

若希望模型**严格按字段**输出（减少漏标题、活动编号错乱），在 **LLM「课堂草案」** 节点：

1. 打开 **结构化输出 / Structured Output**（或「JSON Schema」）
2. 将下方 Schema **整段粘贴**（Dify 1.14+ 在「编辑 JSON Schema」里粘贴即可）
3. **系统提示词**可改为：「你必须只输出符合 Schema 的 JSON，不要 Markdown，不要 \`\`\` 代码块。」
4. **结束节点**仍输出变量名 **`text`**，值为 `{{课堂草案.text}}`（Dify 会把结构化结果放在 LLM 的 `text` 里，多为 JSON 字符串）

学习空间前后端已兼容：**JSON 结构化** 与 **Markdown 三板块** 二选一即可。

**JSON Schema（复制到 Dify）：**

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["objectives", "evaluation", "activities"],
  "properties": {
    "objectives": {
      "type": "string",
      "description": "学习目标：2～3条，学生主语（我能…），每条一行，用换行分隔"
    },
    "evaluation": {
      "type": "string",
      "description": "学习评价：本课评价标准、自评要点、合格标准，与学习目标呼应"
    },
    "activities": {
      "type": "array",
      "minItems": 2,
      "maxItems": 3,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["title", "steps"],
        "properties": {
          "title": {
            "type": "string",
            "description": "活动标题，如：认识身边的系统"
          },
          "purpose": {
            "type": "string",
            "description": "活动目的，1～2 句"
          },
          "steps": {
            "type": "string",
            "description": "简要步骤，可分点，适合 8～15 分钟课堂"
          },
          "duration_minutes": {
            "type": "integer",
            "description": "建议时长（分钟），8～15"
          }
        }
      }
    }
  }
}
```

**结构化时的用户提示词示例（可贴在 LLM 用户消息里）：**

```text
册次：{{grade_name}}
课时：{{lesson_title}}
教材片段：
{{知识检索.result}}

请根据教材片段撰写本课课堂草案。只输出 JSON，字段 objectives、evaluation、activities（2～3 个活动）。
```

**注意：**

| 项 | 说明 |
|----|------|
| 模型 | 优先 **硅基流动** 等支持 JSON Schema 的模型；部分模型结构化不稳定时可继续用 Markdown 方案 |
| 结束节点 | 保持 `text`，不要改成多个输出字段（除非你愿意改后端） |
| 发布 | 改 Schema 或 Prompt 后必须点 **发布** |

### 三至六年级（8 册）如何配置

学习空间会把教师所选课时的 **册次名** 传给 Dify（`grade_name` 如 `四年级下册`），**平台已支持 3～6 年级全部课时**；AI 是否准确取决于知识库是否覆盖该册。

**方案 A：一个知识库 + 8 个 PDF（推荐入门）**

1. Dify → 知识库 → 新建「信息科技 3-6 年级教材」
2. 上传 8 个 PDF（分段建议 **400～500 字/段**，避免 embedding 413）：
   - 三年级上册 / 三年级下册
   - 四年级上册 / 四年级下册
   - 五年级上册 / 五年级下册
   - 六年级上册 / 六年级下册
3. 【活动推荐】工作流 → **知识检索** 节点选中该知识库
4. 检索查询保持：`{{grade_name}} {{lesson_title}} 学习目标 学习活动 学习评价`
5. LLM Prompt 中强调：**仅使用与 {{grade_name}} 匹配的检索片段**，勿混用其他年级

**方案 B：8 个独立知识库（检索更准、维护量大）**

为每册建单独知识库（如「三年级上册信息科技」），工作流用 **问题分类器** 或 **IF 条件** 按 `grade_name` 分支到对应检索节点（你曾用的五下/六上分流可扩展为 8 路，但维护成本高）。

**方案 C：一个知识库 + 文档元数据（8 册混库时强烈推荐）**

8 册 PDF 放在**同一个**知识库时，检索很容易抽到**其他年级**的片段（例如五上 115k 字，片段多、易被命中）。  
此时 **不必用问题分类器**，更稳妥的是 **元数据过滤**：

1. 知识库 → 每个文档（如 `四年级下册.pdf`）→ **元数据** 增加字段：
   - `grade_name` = `四年级下册`（与平台传入的 `grade_name` 完全一致）
2. 【活动推荐】→ **知识检索** 节点 → 开启 **元数据过滤**：
   - 条件：`grade_name` **等于** `{{grade_name}}`（开始节点变量）
3. 检索查询仍为：`{{grade_name}} {{lesson_title}} 学习目标 学习活动 学习评价`
4. 建议 **Top K = 4～6**，开启 **Rerank**（硅基流动 bce-reranker 等）

在 Dify **召回测试**里用「四年级下册 + 某课标题」试检索，确认命中的片段来自该册 PDF，再测工作流。

### 召回测试失败：「渲染此组件时发生了意外错误」

这是 Dify **前端**在展示检索结果时崩溃，不等于知识库一定坏了。按下面顺序排查：

| 步骤 | 操作 |
|------|------|
| 1 | **先关掉元数据过滤**：知识库 → 召回测试（或工作流里知识检索节点）→ 暂时不要勾选「按元数据过滤」，只用查询词测 |
| 2 | **用完整查询句**，不要只输一个字：例如 `五年级下册 第一单元 生活中的系统 第1课 身边的系统 学习目标` |
| 3 | 确认 8 个 PDF 状态均为 **可用**（索引完成）；若某册失败，检查分段是否 **400～500 字**（避免 embedding 413） |
| 4 | 若「不关过滤」就报错、关了能出结果 → 说明 **元数据未配齐或字段名不一致**：每个文档元数据 `grade_name` 必须与平台一致（如 `五年级下册`，不要多空格、不要用「5年级下」） |
| 5 | 召回测试页若有过滤条件，测试时 **手动填** `grade_name = 五年级下册`（与文档元数据相同） |
| 6 | 仍报错时看 Dify 日志：`cd deploy/dify/docker && docker compose logs api --tail=80` |

**建议流程**：先无过滤确认能召回 → 再给每个 PDF 加 `grade_name` 元数据 → 工作流检索节点开启过滤 → 再测工作流。

### 不准确时，先查工作流结构（比分类器更关键）

你当前类似结构：

```text
开始 → 知识检索 → LLM「教学目标」→ LLM「生成活动五下」→ 输出
```

常见问题：

| 问题 | 后果 |
|------|------|
| 第二个 LLM 名叫「生成活动五下」 | Prompt 仍按五下写，换年级也不准 |
| 链式两个 LLM | 第二节点**看不到**知识检索原文，只看见第一个 LLM 的摘要，易丢细节、串年级 |
| 8 册混库且无元数据过滤 | 检索片段来自错误册次 |
| 分段用「通用」且过长 | 命中整册无关段落 |

**推荐改为（无需问题分类器）：**

```text
开始(grade_name, lesson_title)
  → 知识检索（信息科技课程教材 + 元数据 grade_name 过滤）
  → LLM「课堂草案」（一次输出 ## 学习目标 / ## 学习活动 / ## 学习评价）
  → 结束(text = {{LLM.text}})
```

学习空间只读结束节点的 **`text`**，一个 LLM 即可。

### 要不要「问题分类器」？

| 场景 | 建议 |
|------|------|
| 平台已传 `grade_name`（本系统已传） | **不需要**分类器 |
| 8 册在一个知识库 | 用 **元数据过滤**，不要 8 路分类 |
| 8 个独立知识库 | 可用 **IF/条件分支** 按 `grade_name` 选库（8 路，难维护） |
| 用户只输入自然语言、没有册次变量 | 才考虑分类器 |

**结论：不准确的主因通常是「混库检索 + 多 LLM 串联 + 五下专用 Prompt」，而不是少了一个分类器。**

**互动页生成【互动页生成】应用**

与年级无关，只接收 `lesson_title`、`student_objectives`、`activity_title`、`activity_content`，**无需按年级拆工作流**。

**学习空间侧（无需改代码）**

| 年级 | 活动编辑选教材 | 后端传 Dify |
|------|----------------|-------------|
| 三～六年级 | 下拉 8 册均可选 | `grade_name` + `lesson_title`（含单元名） |

若某册 AI 内容不准：检查该册 PDF 是否已入库、索引是否为「可用」、工作流是否已 **发布**。

5. **与工作流旧版差异（若你曾用「问题分类器 + 教学目标 + 生成活动」）**

| 旧做法 | 现做法 |
|--------|--------|
| 多个 LLM、分类器 | **一个** LLM 输出三板块 Markdown |
| 工作流内生成互动页 | 互动页在平台点「生成活动」，走【互动页生成】应用 |
| 输出教学目标/活动分开节点 | 合并为一段 `text`，平台自动拆分 |

修改后务必点击 **发布**，否则 API 仍用旧版。

6. 发布后在 **访问 API** 复制新的 **API Key**（Workflow 与 Chatflow 的 Key 不同），写入项目根目录 `.env`：

```bash
DIFY_RECOMMEND_API_KEY=app-新的Workflow密钥
docker compose up -d --force-recreate backend
```

学习空间后端传入的 `inputs` 示例：

```json
{
  "grade_name": "五年级下册",
  "lesson_title": "第一单元 生活中的系统 第1课 身边的系统"
}
```

### 互动页生成 Workflow（方案 A 第二步，可选）

教师发布时，活动槽若选 **「AI 生成（Dify）」**，会调用第二个 Workflow 生成单活动 HTML；未配置 Key 时自动回退为平台内置模板。

1. Dify 新建 **工作流** 应用，名称如「互动页生成」
2. **开始** 节点输入字段：

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `lesson_title` | 文本 | 课时标题 |
| `student_objectives` | 文本 | 学生学习目标 |
| `activity_title` | 文本 | 活动标题 |
| `activity_content` | 文本 | 活动描述 |

3. **LLM** 提示词要求输出**完整单文件 HTML**（内联 CSS/JS，含简单交互与提交按钮）
4. **结束** 节点输出变量名设为 `html`（或 `text`）
5. 复制 API Key 到 `.env`：

```bash
DIFY_INTERACTIVE_API_KEY=app-互动页Workflow密钥
docker compose up -d --force-recreate backend
```

### 飞象老师 HTML 上传

活动槽选 **「上传飞象网页」** 时，教师上传 `.html` 文件；发布后会复制到班级目录并在学生五板块壳页的对应活动 iframe 中加载。飞象页若含 `postMessage({type:'LEARN_SPACE_SUBMIT',...})` 可与学习空间提交对接。

### 工作流 HTTP 串联另一应用（方案 2）与 SSRF 报错

若在【活动推荐】里用 **HTTP 请求** 调用【生成互动页】，报错：

`Access to 'http://host.docker.internal:8090/...' was blocked by SSRF protection`

原因：Dify 默认经 Squid 代理出站，**禁止访问内网/本机地址**。

**推荐 URL（不要用 host.docker.internal）：**

```text
http://nginx/v1/workflows/run
```

**Header：** `Authorization: Bearer app-生成互动页的密钥`

**Body 示例：**

```json
{
  "inputs": {
    "lesson_title": "{{#开始.lesson_title#}}",
    "student_objectives": "{{#教学目标.text#}}",
    "activity_title": "活动1",
    "activity_content": "{{#生成活动五下.text#}}"
  },
  "response_mode": "blocking",
  "user": "workflow-chain"
}
```

修改 `deploy/dify/docker/ssrf_proxy/squid.conf.template` 已加入内网放行规则后，在 Dify 目录执行：

```bash
cd deploy/dify/docker
docker compose restart ssrf_proxy
```

更稳妥的做法仍是 **方案 1**：两个应用分开，由学习空间在教师「发布」时调用【生成互动页】（配置 `DIFY_INTERACTIVE_API_KEY` 即可）。
