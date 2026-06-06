# WeChat Authorization Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement real WeChat code login in the mini program with a Spring Boot/Java and MySQL backend.

**Architecture:** The frontend owns `wx.login`, Token persistence, authenticated requests, and profile completion UI. The Java backend exposes `/api/auth/wechat/login`, calls `jscode2session`, upserts a MySQL user through a unique `(appid, openid)` key, and issues a business Token without exposing `openid` or `session_key`.

**Tech Stack:** Vue 3, TypeScript, Pinia, uni-app; MySQL 8; Spring Boot, WebClient, MyBatis-Plus, Sa-Token.

---

### Task 1: Frontend authentication contracts and Service

**Files:**
- Create: `src/types/auth.ts`
- Create: `src/services/authStorage.ts`
- Create: `src/services/httpClient.ts`
- Create: `src/services/wechatAuthService.ts`
- Test: `src/services/wechatAuthService.spec.ts`

- [ ] Write failing tests for successful login, missing `wx.login` code, backend rejection, Token persistence, and 401 Token clearing.
- [ ] Run `npm test -- src/services/wechatAuthService.spec.ts` and verify the missing modules fail.
- [ ] Implement typed login contracts, injected WeChat/request adapters, Token storage, and Bearer request behavior.
- [ ] Re-run the focused test and verify it passes.

### Task 2: Pinia login state and login page integration

**Files:**
- Modify: `src/types/inventory.ts`
- Modify: `src/stores/inventoryStore.ts`
- Modify: `src/components/LoginOverlay.vue`
- Modify: `src/components/ProfileView.vue`
- Test: `src/stores/inventory.spec.ts`

- [ ] Write failing store tests for authenticated user restoration, login success, and logout Token clearing.
- [ ] Run the focused store test and verify the new expectations fail.
- [ ] Extend auth state/actions and replace the mock timer login with `wechatAuthService.login()`.
- [ ] Add compatible `chooseAvatar` and `input type="nickname"` profile controls without treating profile data as authentication.
- [ ] Re-run tests and TypeScript checking.

### Task 3: Shared MySQL schema and API documentation

**Files:**
- Create: `backend/mysql/001_wechat_users.sql`
- Create: `backend/README.md`

- [ ] Define the user table, unique/index constraints, timestamps, and optional encrypted session-key column.
- [ ] Document environment variables, request/response payloads, error codes, and secure logging rules.
- [ ] Validate SQL syntax manually and scan documentation for hard-coded secrets.

### Task 4: Java Spring Boot core implementation

**Files:**
- Create: `backend/java/pom.xml`
- Create: `backend/java/src/main/resources/application.yml`
- Create: `backend/java/src/main/java/com/cunmo/auth/AuthApplication.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/config/WechatProperties.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/config/WebClientConfig.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/controller/WechatAuthController.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/service/WechatAuthService.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/service/WechatApiClient.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/repository/WechatUserMapper.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/model/WechatUser.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/model/AuthContracts.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/error/ApiException.java`
- Create: `backend/java/src/main/java/com/cunmo/auth/error/GlobalExceptionHandler.java`
- Create: `backend/java/.env.example`

- [ ] Implement configuration-bound WeChat credentials and timeout-aware WebClient.
- [ ] Implement MyBatis-Plus user lookup/upsert with duplicate-key recovery.
- [ ] Implement Sa-Token login and protocol-compatible response.
- [ ] Implement stable error responses and sanitized logging.
- [ ] Run `mvn test` or at minimum `mvn -DskipTests package` when Maven dependencies are available.

### Task 5: End-to-end verification

**Files:**
- Modify as required by verification findings only.

- [ ] Run `npm test`.
- [ ] Run `npm run build:mp-weixin`.
- [ ] Scan source and examples for embedded app secret, session key logging, or Token logging.
- [ ] Verify Java response fields exactly match frontend `AuthLoginResponse`.
- [ ] Run the Java backend build and report any environment limitations.
