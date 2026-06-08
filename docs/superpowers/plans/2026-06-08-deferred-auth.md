# Deferred Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let guests browse representative local inventory data, require login only at protected actions, resume the requested action after login, and return to guest mode after logout or session expiry.

**Architecture:** Add a focused guest-data module and model pending authentication as serializable intents in Pinia. Route protected UI entry points through store actions, keep the existing WeChat login service, and let the HTTP client notify the store when a 401 invalidates the session.

**Tech Stack:** Vue 3, TypeScript, Pinia, uni-app, Vitest

---

### Task 1: Guest experience data

**Files:**
- Create: `src/services/guestInventory.ts`
- Test: `src/services/guestInventory.spec.ts`

- [ ] **Step 1: Write failing tests**

Test that each call creates independent sample data and that filtering by space, category, and keyword returns the expected sample items.

- [ ] **Step 2: Verify RED**

Run `npx vitest run src/services/guestInventory.spec.ts`.
Expected: FAIL because `guestInventory` does not exist.

- [ ] **Step 3: Implement guest data**

Export `createGuestInventory()` and `filterGuestItems()` with fixed bootstrap, item, analytics, and transaction data matching existing inventory types.

- [ ] **Step 4: Verify GREEN**

Run `npx vitest run src/services/guestInventory.spec.ts`.
Expected: PASS.

### Task 2: Store authentication state and intent continuation

**Files:**
- Modify: `src/types/inventory.ts`
- Modify: `src/stores/inventoryStore.ts`
- Modify: `src/stores/inventory.spec.ts`

- [ ] **Step 1: Write failing store tests**

Cover guest startup without API calls, protected view and modal intents, cancel behavior, login continuation after real inventory initialization, direct authenticated actions, no forced profile redirect, logout recovery to guest data, and 401 recovery.

- [ ] **Step 2: Verify RED**

Run `npx vitest run src/stores/inventory.spec.ts`.
Expected: FAIL on missing guest and authentication intent behavior.

- [ ] **Step 3: Implement state and actions**

Add `authMode`, `loginVisible`, `pendingAuthIntent`, `noticeMessage`, and a private guest source list. Implement guest loading/filtering, `requestAuthentication`, `cancelAuthentication`, async `completeLogin`, `continuePendingIntent`, protected dispatch behavior, and logout/session-expiry reset.

- [ ] **Step 4: Verify GREEN**

Run `npx vitest run src/stores/inventory.spec.ts`.
Expected: PASS.

### Task 3: HTTP 401 state notification

**Files:**
- Modify: `src/services/httpClient.ts`
- Modify: `src/services/wechatAuthService.spec.ts`

- [ ] **Step 1: Write failing test**

Assert that a 401 clears storage and invokes an injected unauthorized callback exactly once.

- [ ] **Step 2: Verify RED**

Run `npx vitest run src/services/wechatAuthService.spec.ts`.
Expected: FAIL because the client has no unauthorized callback.

- [ ] **Step 3: Implement callback**

Allow `createAuthHttpClient` to receive `onUnauthorized`, and expose `setUnauthorizedHandler` for the application singleton.

- [ ] **Step 4: Verify GREEN**

Run `npx vitest run src/services/wechatAuthService.spec.ts`.
Expected: PASS.

### Task 4: Protected UI entry points and logout

**Files:**
- Modify: `src/pages/index/index.vue`
- Modify: `src/components/LoginOverlay.vue`
- Modify: `src/components/ProfileView.vue`
- Modify: `src/components/HeaderBar.vue`
- Modify: `src/components/BottomNav.vue`
- Modify: `src/components/ItemCard.vue`
- Modify: `src/components/ItemList.vue`
- Modify: `src/components/EmptyCavityPanel.vue`
- Modify: `src/components/SpaceNavigator.vue`

- [ ] **Step 1: Wire page lifecycle**

Initialize either the restored authenticated session or guest experience, register the 401 handler, show the login overlay only when requested, and display a guest-data/expiry notice.

- [ ] **Step 2: Make login dismissible**

Await store login completion, add “暂不登录，继续浏览”, and present the component as a modal sheet over the current screen.

- [ ] **Step 3: Route protected interactions**

Use store-protected dispatch/actions for profile entry, create actions, stock adjustment, and deletion management. Keep browsing, filtering, searching, lens switching, and timeline navigation available to guests.

- [ ] **Step 4: Add confirmed logout**

Show a confirmation dialog in `ProfileView`; after confirmation call store logout, which returns to the home guest experience without reopening login.

### Task 5: Full verification

**Files:**
- Modify as required by verification findings only.

- [ ] **Step 1: Run all tests**

Run `npm test`.
Expected: all tests pass.

- [ ] **Step 2: Run type and H5 build**

Run `npm run build`.
Expected: exit 0.

- [ ] **Step 3: Run WeChat mini-program build**

Run `npm run build:mp-weixin`.
Expected: exit 0.

- [ ] **Step 4: Inspect diff**

Run `git diff --check` and review `git diff --stat`.
Expected: no whitespace errors and only scoped files changed.
