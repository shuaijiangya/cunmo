# 存魔项目 README Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增一份兼顾项目接手与技术评审的根目录 README，并确保所有说明与当前代码一致。

**Architecture:** 根 README 作为全项目入口，先描述运行方式，再解释前后端架构和 Java 设计取舍。后端专项细节继续由 `backend/README.md` 承担，根 README 通过链接关联，避免重复维护全部接口协议。

**Tech Stack:** Markdown、Mermaid、npm、uni-app、Vue 3、Java 21、Spring Boot、Maven、MySQL。

---

### Task 1: 编写根目录 README

**Files:**
- Create: `README.md`
- Reference: `package.json`
- Reference: `backend/java/pom.xml`
- Reference: `backend/java/cunmo-bootstrap/src/main/resources/application.yml`
- Reference: `backend/mysql/*.sql`

- [x] **Step 1: 写入项目入口信息**

写入项目定位、当前能力、技术栈和目录结构，明确前端为 uni-app/Vue 小程序，后端为 Java DDD 模块化单体。

- [x] **Step 2: 写入可执行的快速启动步骤**

给出以下命令和前置条件：

```bash
npm install
npm run dev
npm run dev:mp-weixin
npm test
npm run build
npm run build:mp-weixin
```

Java 端说明 Java 21、Maven、MySQL 8.0+，数据库脚本按 `001`、`002`、`003`、`005` 顺序执行，`004` 仅用于本地联调。提供 `.env` 配置和 Maven 构建、测试、启动命令。

- [x] **Step 3: 写入架构与设计理由**

使用 Mermaid 表达请求调用链，并解释：

- 为什么选择模块化单体而非微服务。
- 为什么领域层保持框架无关。
- 为什么查询模型直接面向页面结果。
- 为什么使用 `vault_id` 隔离。
- 为什么不使用数据库外键。
- 为什么使用逻辑删除和不可变流水快照。
- 为什么使用 Sa-Token 和端口适配。

- [x] **Step 4: 写入 Java 当前问题与形成原因**

使用“现状、原因、影响、建议”表格记录当前可验证问题：

- RBAC 数据存在但接口尚未执行权限码校验。
- Sa-Token 登录态尚未接入 Redis。
- 头像使用本地文件系统。
- SQL 迁移依赖人工执行。
- 基础设施直接依赖应用异常和查询结果类型。
- 库存仓储实现与控制器文件偏大。
- 测试覆盖不均且当前有一个读模型测试被整体注释。
- 登录注册流程缺少清晰的整体事务边界。
- 监控、健康检查、接口文档和容器化尚未建立。

- [x] **Step 5: 写入开发规范和文档索引**

链接 `backend/README.md`、已有设计文档与实施计划，并说明 Java 中文 Javadoc、敏感信息日志和前端状态边界。

### Task 2: 校验 README

**Files:**
- Verify: `README.md`
- Verify: `docs/superpowers/specs/2026-06-07-project-readme-design.md`

- [x] **Step 1: 检查 Markdown 与占位符**

Run:

```bash
rg -n "TBD|TODO|待补充|replace_me|your_appid" README.md
git diff --check -- README.md docs/superpowers/specs/2026-06-07-project-readme-design.md docs/superpowers/plans/2026-06-07-project-readme.md
```

Expected: README 不包含待完成占位符，`git diff --check` 无输出。

- [x] **Step 2: 校验命令、路径和版本事实**

Run:

```bash
test -f package.json
test -f backend/java/pom.xml
test -f backend/java/cunmo-bootstrap/src/main/resources/application.yml
for file in backend/mysql/001_wechat_users.sql backend/mysql/002_rbac_seed.sql backend/mysql/003_inventory_domain.sql backend/mysql/004_inventory_seed.sql backend/mysql/005_inventory_deletion.sql; do test -f "$file"; done
```

Expected: 命令退出码为 0。

- [x] **Step 3: 运行项目现有验证**

Run:

```bash
npm test
npm run build
cd backend/java && mvn test
```

Expected: 前端测试和构建通过；Java 测试通过。若 Java 测试因用户当前未提交的测试改动或本地基础设施失败，记录具体原因，不修改无关文件。

实际结果：前端 22 个测试通过，H5 与微信小程序构建通过；当前终端缺少
`mvn` 命令，因此未能执行 Java 测试。
