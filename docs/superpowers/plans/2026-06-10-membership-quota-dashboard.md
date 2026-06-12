# Membership Quota Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the quota dashboard, PRO membership lifecycle, payment/renewal contracts, administrator upgrade APIs, and strict FREE quota enforcement.

**Architecture:** A new membership bounded context owns products, entitlements, orders, renewal agreements, and upgrade requests. Inventory consumes a small quota policy port, while payment provider details remain behind an application port. The uni-app page consumes one dashboard contract shared exactly with Java response records.

**Tech Stack:** Java 21, Spring Boot 3.4, MyBatis-Plus, MySQL 8, Vue 3, uni-app, TypeScript, Vitest.

---

### Task 1: Membership Domain

**Files:**
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/membership/**`
- Test: `backend/java/cunmo-domain/src/test/java/cn/cunmo/domain/membership/model/aggregate/MembershipEntitlementTest.java`

- [x] Write failing tests for monthly activation, extension, expiry, lifetime precedence, and quota decisions.
- [x] Run the domain test and confirm it fails because membership classes do not exist.
- [x] Implement membership enums, product definitions, entitlement aggregate, and quota policy value object.
- [x] Run the domain test and confirm it passes.

### Task 2: API Contract and Application Services

**Files:**
- Create: `backend/java/cunmo-api/src/main/java/cn/cunmo/api/membership/**`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/membership/**`
- Test: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/membership/service/MembershipApplicationServiceTest.java`

- [x] Write service tests for payment completion/idempotency, renewal termination and expiry convergence.
- [x] Implement repository/query/payment ports, result records, commands, and application services.
- [x] Verify focused application tests pass.

### Task 3: Persistence and Database Migration

**Files:**
- Create: `backend/mysql/006_membership_quota.sql`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/membership/**`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/config/MembershipProperties.java`
- Modify: `backend/java/cunmo-bootstrap/src/main/resources/application.yml`
- Modify: `backend/java/cunmo-bootstrap/src/main/java/cn/cunmo/bootstrap/CunmoApplication.java`

- [x] Add schema tables and indexes for entitlements, orders, agreements, attempts, notifications, and upgrade requests.
- [x] Implement MyBatis data objects, mappers, repository, dashboard queries and expiry convergence.
- [x] Add contact, payment, and renewal configuration with renewal feature gating.

### Task 4: Payment and Renewal Adapters

**Files:**
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/payment/wechat/**`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/membership/service/MembershipRenewalService.java`
- Create: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/controller/WechatPayCallbackController.java`

- [x] Implement JSAPI API v3 prepay request creation behind `MembershipPaymentGateway`.
- [x] Implement API v3 callback signature verification, identity validation and AES-GCM resource decryption.
- [x] Implement V2 XML/HMAC renewal signing, callbacks, charge/termination hooks, retries and idempotency.
- [x] Keep automatic renewal hidden when required merchant configuration is incomplete.

### Task 5: Strict Inventory Quotas

**Files:**
- Modify: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/repository/InventoryRepository.java`
- Modify: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/service/InventoryApplicationService.java`
- Modify: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/**`
- Test: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/inventory/service/InventoryQuotaApplicationServiceTest.java`

- [x] Write tests for 3 root spaces, 3 categories per space, 10 item records per cavity, and unlimited PRO access.
- [x] Add locked count queries and inject the membership quota policy.
- [x] Enforce quotas in create-space, create-category, update-bindings, and create-item transactions.
- [x] Map quota failures to stable HTTP 409 responses.

### Task 6: HTTP Controllers

**Files:**
- Create: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/controller/MembershipController.java`
- Create: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/controller/AdminMembershipController.java`
- Test: `backend/java/cunmo-trigger/src/test/java/cn/cunmo/trigger/http/controller/MembershipControllerTest.java`

- [x] Write MockMvc contract tests.
- [x] Implement user, callback, and administrator endpoints.
- [x] Add administrator permission checks and HTTP response mapping.
- [x] Verify JSON field names match the TypeScript contract.

### Task 7: Frontend Data Contract and API

**Files:**
- Create: `src/types/membership.ts`
- Create: `src/services/membershipApi.ts`
- Test: `src/services/membershipApi.spec.ts`

- [x] Write request-contract tests.
- [x] Implement dashboard, order polling, renewal, and upgrade request calls.
- [x] Verify Vitest passes.

### Task 8: High-Fidelity Quota Page

**Files:**
- Create: `src/pages/quota/index.vue`
- Create: `src/services/membershipPayment.ts`
- Test: `src/services/membershipPayment.spec.ts`
- Modify: `src/pages.json`
- Modify: `src/components/ProfileView.vue`

- [x] Write payment orchestration tests for requestPayment, cancellation, and authoritative order polling.
- [x] Implement the dynamic quota page from the approved prototype.
- [x] Reproduce colors, radii, shadows, and the 2.5-second shimmer animation.
- [x] Add monthly/lifetime selection, renewal controls, upgrade request form, customer service, QR preview, phone actions, loading and error states.
- [x] Add a profile navigation entry.

### Task 9: Verification

**Files:**
- Modify: `backend/java/.env.example`
- Modify: `.env.example` only if frontend configuration is required.

- [x] Run `npm test`.
- [x] Run `npm run build:mp-weixin`.
- [ ] Run `mvn test` in `backend/java` when Maven is available (Maven is intentionally not installed/downloaded in this workspace).
- [x] Inspect the generated mini-program page and compiled output.
- [x] Run `git diff --check` and review the full change set against the design.

Focused Java verification on 2026-06-12 used Java 21, the existing local dependency cache,
`javac`, and JUnit Platform Launcher: 12 membership/quota/controller tests passed. This does
not replace the normal Maven reactor test run in an environment where Maven is available.
