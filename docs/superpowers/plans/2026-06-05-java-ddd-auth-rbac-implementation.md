# Java DDD Authentication and RBAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the Java backend into a Java 21 DDD modular monolith that implements WeChat login, idempotent user registration, multi-role RBAC, permission loading, and Sa-Token authentication.

**Architecture:** A Maven parent aggregates API, Domain, Application, Infrastructure, Trigger, and Bootstrap modules. The domain owns user and authorization rules, application services orchestrate login, infrastructure implements MySQL/WeChat/Sa-Token ports, and trigger adapts HTTP requests without accessing persistence directly.

**Tech Stack:** Java 21, Spring Boot 3.4, Maven, MyBatis-Plus, MySQL 8, Sa-Token, WebClient, JUnit 5, Mockito.

---

### Task 1: Maven Aggregator and Module Boundaries

**Files:**
- Replace: `backend/java/pom.xml`
- Create: `backend/java/cunmo-api/pom.xml`
- Create: `backend/java/cunmo-domain/pom.xml`
- Create: `backend/java/cunmo-application/pom.xml`
- Create: `backend/java/cunmo-infrastructure/pom.xml`
- Create: `backend/java/cunmo-trigger/pom.xml`
- Create: `backend/java/cunmo-bootstrap/pom.xml`
- Delete: `backend/java/src`

- [ ] Create a Java 21 parent POM with dependency management and six modules.
- [ ] Define one-way module dependencies exactly as specified in the design.
- [ ] Add Maven Wrapper so the build does not depend on a globally installed Maven.
- [ ] Run `./mvnw -q -DskipTests package` and verify the empty reactor builds.

### Task 2: API and Domain Model

**Files:**
- Create API request/response records under `cunmo-api/src/main/java/cn/cunmo/api/auth`.
- Create user aggregate, role, permission, WeChat identity, enums, values, ports, and exceptions under `cunmo-domain/src/main/java/cn/cunmo/domain/auth`.
- Test: `cunmo-domain/src/test/java/cn/cunmo/domain/auth/model/aggregate/UserTest.java`

- [ ] Write failing tests for enabled user creation, disabled-user rejection, and profile completeness.
- [ ] Implement immutable value objects and the User aggregate without framework annotations.
- [ ] Define repository and gateway interfaces using domain types only.
- [ ] Run domain tests and verify they pass.

### Task 3: Application Login Use Case

**Files:**
- Create command, result, assembler, token port, and application service under `cunmo-application/src/main/java/cn/cunmo/application/auth`.
- Test: `cunmo-application/src/test/java/cn/cunmo/application/auth/service/WechatLoginApplicationServiceTest.java`

- [ ] Write failing tests for existing login, first registration, disabled user, and role/permission response.
- [ ] Implement the application service so the external WeChat call occurs before repository transaction work.
- [ ] Load distinct active role and permission codes and issue a Token through `TokenService`.
- [ ] Run application tests and verify they pass.

### Task 4: Commented MySQL RBAC Schema

**Files:**
- Replace: `backend/mysql/001_wechat_users.sql`
- Create: `backend/mysql/002_rbac_seed.sql`

- [ ] Create `sys_user`, `sys_wechat_identity`, `sys_role`, `sys_permission`, `sys_user_role`, and `sys_role_permission`.
- [ ] Add a Chinese table comment and Chinese column comment for every business table and field.
- [ ] Add unique/index constraints for WeChat identity, roles, permissions, and authorization lookups.
- [ ] Seed `USER`, `ADMIN`, and inventory permissions idempotently.

### Task 5: Infrastructure Adapters

**Files:**
- Create persistence DOs, mappers, converters, and repository adapters under `cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/user`.
- Create WeChat gateway under `cn.cunmo.infrastructure.gateway.wechat`.
- Create Sa-Token adapter under `cn.cunmo.infrastructure.security.satoken`.
- Create configuration under `cn.cunmo.infrastructure.config`.
- Test infrastructure mapping and WeChat error behavior.

- [ ] Implement transactional find-or-register with duplicate-key recovery.
- [ ] Implement role and permission queries with distinct active codes.
- [ ] Implement timeout-aware `jscode2session` mapping without sensitive logs.
- [ ] Implement Sa-Token login and return its value and remaining lifetime.
- [ ] Run infrastructure tests.

### Task 6: HTTP Trigger and Bootstrap

**Files:**
- Create controller, converter, and global exception handler under `cunmo-trigger/src/main/java/cn/cunmo/trigger/http`.
- Create `cn.cunmo.bootstrap.CunmoApplication`.
- Move configuration to `cunmo-bootstrap/src/main/resources/application.yml`.
- Move `.env.example` values to the Java backend root example.
- Test: MockMvc controller contract test.

- [ ] Expose `POST /api/auth/wechat/login`.
- [ ] Return stable error codes and the exact frontend response contract.
- [ ] Configure component and mapper scanning from `cn.cunmo`.
- [ ] Run trigger and bootstrap tests.

### Task 7: Frontend Authorization Contract

**Files:**
- Modify: `src/types/auth.ts`
- Modify: `src/services/authStorage.ts`
- Modify: `src/services/wechatAuthService.spec.ts`

- [ ] Write failing tests for role/permission persistence and legacy-session normalization.
- [ ] Add `roles` and `permissions` to the authenticated user contract.
- [ ] Normalize missing arrays when restoring an older cached session.
- [ ] Run frontend tests and the WeChat mini-program build.

### Task 8: End-to-End Verification

**Files:**
- Modify only files required by verification findings.

- [ ] Run `./mvnw test` under `backend/java`.
- [ ] Run `npm test`.
- [ ] Run `npm run build:mp-weixin`.
- [ ] Scan all Java packages and reject anything not starting with `cn.cunmo`.
- [ ] Scan for Controller-to-Mapper coupling and Domain framework dependencies.
- [ ] Scan for hard-coded secrets and logging of code, openid, session key, or Token.
