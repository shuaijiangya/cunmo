# Auth Monitoring And Logout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Micrometer login/logout metrics, a current-token-only Java logout endpoint, Actuator Prometheus exposure, and best-effort backend logout from the mini program.

**Architecture:** Application owns authentication monitoring and logout orchestration through ports. Infrastructure adapts those ports to Micrometer and Sa-Token, Trigger exposes HTTP, and the frontend always clears local state even when remote logout fails.

**Tech Stack:** Java 21, Spring Boot 3.4, Sa-Token, Micrometer, Actuator, Prometheus, Vue 3, Pinia, Vitest

---

### Task 1: Authentication monitoring port and login instrumentation

**Files:**
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/auth/port/AuthMonitoring.java`
- Modify: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/auth/service/WechatLoginApplicationService.java`
- Modify: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/auth/service/WechatLoginApplicationServiceTest.java`

- [ ] Write failing tests that verify successful login records `loginSucceeded(duration)`, known domain errors record their stable code, and unexpected errors record `INTERNAL_ERROR`.
- [ ] Run the application auth test and confirm failure because monitoring is absent.
- [ ] Add the monitoring port and instrument the full login use case while preserving original exceptions.
- [ ] Re-run the application auth test and confirm it passes.

### Task 2: Current-token logout application use case

**Files:**
- Modify: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/auth/port/TokenService.java`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/auth/service/LogoutApplicationService.java`
- Create: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/auth/service/LogoutApplicationServiceTest.java`

- [ ] Write failing tests proving the service reads the user before logout, records success, and records `UNAUTHORIZED` on invalid sessions.
- [ ] Run the logout service test and confirm missing production types.
- [ ] Add `logoutCurrentToken()` and implement the orchestration, structured logs, duration, and stable failure mapping.
- [ ] Re-run the logout service test and confirm it passes.

### Task 3: Micrometer and Sa-Token adapters

**Files:**
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/monitoring/MicrometerAuthMonitoring.java`
- Create: `backend/java/cunmo-infrastructure/src/test/java/cn/cunmo/infrastructure/monitoring/MicrometerAuthMonitoringTest.java`
- Modify: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/security/satoken/SaTokenService.java`
- Modify: `backend/java/cunmo-infrastructure/pom.xml`

- [ ] Write a failing `SimpleMeterRegistry` test for counters, error-code tags, and outcome timers.
- [ ] Run the infrastructure monitoring test and confirm the adapter is missing.
- [ ] Implement bounded metric names/tags and current-token `StpUtil.logout()` behavior.
- [ ] Re-run infrastructure tests and confirm they pass.

### Task 4: Logout HTTP endpoint

**Files:**
- Modify: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/controller/WechatAuthController.java`
- Modify: `backend/java/cunmo-trigger/src/test/java/cn/cunmo/trigger/http/controller/WechatAuthControllerTest.java`

- [ ] Write a failing MockMvc test for `POST /api/auth/logout` returning 204 and invoking the logout service.
- [ ] Run the controller test and confirm the endpoint is absent.
- [ ] Inject `LogoutApplicationService` and add the no-body endpoint.
- [ ] Re-run controller tests and confirm they pass.

### Task 5: Actuator and Prometheus exposure

**Files:**
- Modify: `backend/java/cunmo-bootstrap/pom.xml`
- Modify: `backend/java/cunmo-bootstrap/src/main/resources/application.yml`
- Modify: `README.md`

- [ ] Add Actuator and Prometheus registry runtime dependencies.
- [ ] Expose only `health,info,metrics,prometheus` and document that production ingress must restrict `/actuator`.
- [ ] Verify the effective application configuration compiles in the full Maven reactor.

### Task 6: Frontend best-effort remote logout

**Files:**
- Create: `src/services/logoutService.ts`
- Create: `src/services/logoutService.spec.ts`
- Modify: `src/stores/inventoryStore.ts`
- Modify: `src/stores/inventory.spec.ts`
- Modify: `src/components/ProfileView.vue`

- [ ] Write failing tests proving `POST /api/auth/logout` is attempted with the current session and local logout still happens after success or failure.
- [ ] Run focused Vitest tests and confirm the remote logout behavior is absent.
- [ ] Implement the logout service and async store action; keep the confirmation UI responsive while logout is running.
- [ ] Re-run focused frontend tests and confirm they pass.

### Task 7: Full verification

**Files:**
- Modify only files required by verification findings.

- [ ] Run `npm test`, `npm run build`, and `npm run build:mp-weixin`.
- [ ] Run the Java Maven reactor tests using an available Maven executable or a temporary verified Maven distribution.
- [ ] Run `git diff --check` and inspect the final diff for secrets, Token logging, unbounded metric tags, and user-level logout calls.
