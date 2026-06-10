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

- [ ] Write failing tests for monthly activation, extension, expiry, lifetime precedence, and quota decisions.
- [ ] Run the domain test and confirm it fails because membership classes do not exist.
- [ ] Implement membership enums, product definitions, entitlement aggregate, and quota policy value object.
- [ ] Run the domain test and confirm it passes.

### Task 2: API Contract and Application Services

**Files:**
- Create: `backend/java/cunmo-api/src/main/java/cn/cunmo/api/membership/**`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/membership/**`
- Test: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/membership/service/MembershipApplicationServiceTest.java`

- [ ] Write failing service tests for dashboard mapping, order creation, idempotent payment completion, renewal termination, duplicate upgrade requests, and administrator grants.
- [ ] Implement repository/query/payment ports, result records, commands, and application services.
- [ ] Verify application tests pass.

### Task 3: Persistence and Database Migration

**Files:**
- Create: `backend/mysql/006_membership_quota.sql`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/membership/**`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/config/MembershipProperties.java`
- Modify: `backend/java/cunmo-bootstrap/src/main/resources/application.yml`
- Modify: `backend/java/cunmo-bootstrap/src/main/java/cn/cunmo/bootstrap/CunmoApplication.java`

- [ ] Add schema tables and indexes for entitlements, orders, agreements, attempts, notifications, and upgrade requests.
- [ ] Implement MyBatis data objects, mappers, repository, and dashboard quota queries.
- [ ] Add contact, payment, and renewal configuration with renewal feature gating.

### Task 4: Payment and Renewal Adapters

**Files:**
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/payment/wechat/**`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/membership/service/MembershipRenewalService.java`
- Create: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/controller/WechatPayCallbackController.java`

- [ ] Implement JSAPI prepay request creation behind `MembershipPaymentGateway`.
- [ ] Implement API v3 callback signature verification and AES-GCM resource decryption.
- [ ] Implement renewal agreement creation/termination hooks, due renewal processing, retry accounting, and idempotent callbacks.
- [ ] Keep automatic renewal hidden when required merchant configuration is incomplete.

### Task 5: Strict Inventory Quotas

**Files:**
- Modify: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/repository/InventoryRepository.java`
- Modify: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/service/InventoryApplicationService.java`
- Modify: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/**`
- Test: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/inventory/service/InventoryQuotaApplicationServiceTest.java`

- [ ] Write failing tests for 3 root spaces, 3 categories per space, 10 item records per cavity, and unlimited PRO access.
- [ ] Add locked count queries and inject the membership quota policy.
- [ ] Enforce quotas in create-space, create-category, update-bindings, and create-item transactions.
- [ ] Map quota failures to stable HTTP 409 responses.

### Task 6: HTTP Controllers

**Files:**
- Create: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/controller/MembershipController.java`
- Create: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/controller/AdminMembershipController.java`
- Test: `backend/java/cunmo-trigger/src/test/java/cn/cunmo/trigger/http/controller/MembershipControllerTest.java`

- [ ] Write failing MockMvc contract tests.
- [ ] Implement user, callback, and administrator endpoints.
- [ ] Add administrator permission checks and HTTP response mapping.
- [ ] Verify JSON field names match the TypeScript contract.

### Task 7: Frontend Data Contract and API

**Files:**
- Create: `src/types/membership.ts`
- Create: `src/services/membershipApi.ts`
- Test: `src/services/membershipApi.spec.ts`

- [ ] Write failing request-contract tests.
- [ ] Implement dashboard, order polling, renewal, and upgrade request calls.
- [ ] Verify Vitest passes.

### Task 8: High-Fidelity Quota Page

**Files:**
- Create: `src/pages/quota/index.vue`
- Create: `src/services/membershipPayment.ts`
- Test: `src/services/membershipPayment.spec.ts`
- Modify: `src/pages.json`
- Modify: `src/components/ProfileView.vue`

- [ ] Write failing payment orchestration tests for requestPayment, cancellation, and authoritative order polling.
- [ ] Implement the dynamic quota page from the approved prototype.
- [ ] Reproduce colors, radii, shadows, and the 2.5-second shimmer animation.
- [ ] Add monthly/lifetime selection, renewal controls, upgrade request form, customer service, QR preview, phone actions, loading and error states.
- [ ] Add a profile navigation entry.

### Task 9: Verification

**Files:**
- Modify: `backend/java/.env.example`
- Modify: `.env.example` only if frontend configuration is required.

- [ ] Run `npm test`.
- [ ] Run `npm run build:mp-weixin`.
- [ ] Run `mvn test` in `backend/java` when Maven is available.
- [ ] Inspect the generated mini-program page in the in-app browser or compiled output.
- [ ] Run `git diff --check` and review the full change set against the design.
