# Project Plan: Comprehensive Testing Coverage

## 🎯 Goal
To significantly improve test coverage across the entire dentistry education platform by implementing a layered testing strategy that covers Unit, Integration, and E2E scenarios for the shared logic, student bot, and admin web API.

## 🛠️ Strategy Overview (The Three Pillars of Testing)

### 1. Shared Module - Pure Business Logic (Unit Tests)
**Focus:** The `shared` module must be covered with pure unit tests to ensure business rules are isolated from infrastructure concerns (DB/Telegram).
*   **Action:** Write `@Test` classes for `StateManager`, `TestService`, etc., using Mockito.
*   **Dependencies to Mock:** JDBC interactions (mocking `JdbcTemplate`), external API calls (`gradingClient`).
*   **Pattern:** Use **Dependency Injection and Mock Objects** heavily.

### 2. StudentBot Module - State Machine Logic (Integration Tests)
**Focus:** Verifying state transitions are correct when exposed to mocked updates. These tests must simulate the full input pipeline from Telegram.
*   **Action:** Create test cases that feed mock `Update` objects (containing `callback_data`) into the handlers (`LessonHandler`, `TestHandler`).
*   **Dependencies to Mock:** The incoming message payload structure and any external service calls required during a state transition.
*   **Pattern:** Use **Sealed State Transitions**: Each test case should represent one user action leading to one verifiable new internal state.

### 3. AdminWeb Module - API Endpoints (Integration Tests)
**Focus:** Verifying that REST endpoints handle business logic correctly before hitting the database, and also verifying data persistence upon success.
*   **Action:** Use Spring's `MockMvc` or dedicated test slices (`@WebMvcTest`) to simulate HTTP requests (GET/POST).
*   **Dependencies to Mock:** External APIs (Grading Service) and potentially the entire persistence layer *if* we are testing pure controller logic, otherwise we rely on in-memory DB setups for unit tests.
*   **Pattern:** Test response codes (2xx success, 401 unauthorized, 400 bad request) and data payloads, independent of a running database instance.

## 🗺️ Execution Steps & Implementation Plan

### Phase I: Core Service Unit Tests (`shared` module) - START HERE
1.  **Test `StateManager`**: Write unit tests covering state transitions, saving/loading JSONB data, and ensuring lock management works correctly via mocks.
2.  **Test `TestService`**: Focus on the scoring logic, question retrieval, and grade calculation algorithms using mocked repositories to isolate the service layer.

### Phase II: Handler Unit Tests (`studentbot` module)
1.  Create a set of canonical test scenarios (e.g., "User asks for theory lesson 5", "User submits incorrect answer").
2.  For each scenario, write a handler unit test that feeds a mock update and asserts the final state data and/or outgoing message text.

### Phase III: API Controller Integration Tests (`adminweb` module)
1.  Test `StudentController` endpoints for full CRUD cycle simulation.
2.  Test `ResultController` to ensure grade calculation logic (if exposed via an endpoint) works correctly by mocking the grading service interaction.

## 🧪 Verification Plan
After implementing the tests, the following steps must be taken:
1.  **Run Unit Tests:** `mvn test -pl shared`. Verify all unit tests pass without modification.
2.  **Run Integration Tests:** Run specific modules with `@SpringBootTest` configuration to ensure API endpoints and handlers function correctly in a near-real environment (with mocking of external services).

This plan assumes the development team has standardized utility methods for creating mock update objects and managing transaction boundaries across modules. The next step is to start implementing these tests incrementally, beginning with the `shared` module as it provides the foundation for both bots.

---

## ✅ Implemented Features (All 10 Tasks — Completed 2026-08-11 to 2026-08-13)

### Task 1 — Remove subgroup D
- Removed "D" button from `StudentKeyboards.subgroupSelection()`. Only A, B, C remain.

### Task 2 — Photo upload for questions/tasks (Admin)
- Added `photo_url` column to `questions` and `situational_tasks` tables (V11 migration).
- `AdminWeb` API endpoints expose `photoUrl` field; frontend renders images in question/task forms.

### Task 3 — Language selection RU/UZ (Bot + Admin)
- Registration flow starts with language selection (`REGISTER_LANGUAGE` state → `languageSelection()` inline keyboard).
- Language stored in `students.language` column and carried through `RegData.language` field during registration.
- Profile has "Change language" option; `handleLangCallback()` calls `updateStudentLanguage()`.
- All messages use `Lang.msg(lang, uz, ru)` helper throughout handlers.
- `StudentUpdateDispatcher` matches main menu buttons in both languages.
- `GradingService` instructs AI to respond in the student's recorded language.
- AdminWeb sidebar has UZ/RU toggle (`Segmented` component) — stored in `appLang` React state.

### Task 4 — Fix attempt number display
- `attempt_number` is now read from DB on each result fetch and displayed correctly in bot and AdminWeb.

### Task 5 — Source links in student grade notification
- `GradingService` notification message includes `sourceLinks` field from the graded question.

### Task 6 — Transliteration search (latin/cyrillic)
- `RegistrationHandler.normalizeCyrillicToLatin()` maps look-alike Cyrillic chars to Latin equivalents.
- Admin search normalizes both query and stored values before comparison.

### Task 7 — Group statistics + pass rate (Test Results)
- `TestResultsPage` groups results by `groupName`, calculates pass percentage, renders a summary table.
- Sorted by group name; pass rate uses configurable threshold (default ≥ 60 score).

### Task 8 — Fix AI feedback display in textarea
- Frontend `textarea` now uses `defaultValue` instead of `value` to allow scrollable multi-line display without truncation.

### Task 9 — Role system (SUPER_ADMIN / ZAV_KAFEDRA / PROFESSOR)
- V13 migration: `admin_users` table (`id, username, password_hash, role, full_name, created_at`).
- `AdminUserSeeder` seeds "admin" user from `app.adminPassword` config on first boot if table is empty.
- `SecurityConfig` uses `AdminUserRepository`-backed `UserDetailsService`.
- URL rules: `GET /api/**` → any authenticated; `POST/PUT/DELETE /api/**` → ZAV_KAFEDRA+; `/api/admin-users/**` → SUPER_ADMIN only.
- `AuthController.me()` returns `role` (stripped of `ROLE_` prefix).
- Frontend `AppLayout` fetches role, hides menu items below user's `minRole`.
- `AdminUsersPage` — full CRUD UI for managing admin accounts (SUPER_ADMIN only).

### Task 10 — PDF export of group statistics
- `TestResultsPage` "PDF eksport" button generates printable HTML in a new browser window and calls `window.print()`. No extra npm dependencies.

---

## ✅ Test Coverage Status

### `studentbot` module — Handler Unit Tests (all via `HandlerTestSupport`)
| File | Tests |
|------|-------|
| `RegistrationHandlerTest` | startRegistration sets REGISTER_LANGUAGE state; handleLanguageCallback (uz/ru) sets REGISTER_FULL_NAME + edits message; handleRegistrationStep full name validation; course/group/subgroup/faculty callbacks; Cyrillic normalization; isInRegistration covers all 6 states |
| `ProfileHandlerTest` | showProfile found/not-found; editProfileCallback (course/group/lang); handleLangCallback updates language + shows profile in new lang; course/group/subgroup/faculty edit callbacks; handleProfileEditText |
| Other handler tests | `TestHandlerTest`, `LessonHandlerTest`, `TheoryHandlerTest`, `SituationalHandlerTest` |

### `adminweb` module — Controller Unit Tests (plain Mockito, no Spring context)
| File | Tests |
|------|-------|
| `AuthControllerTest` | login bad creds → 401; login success → session stored; logout invalidates session; me() unauthenticated → 401; me() returns username + role; role stripped of ROLE_ prefix |
| `AdminUserControllerTest` | list returns mapped users; create valid/missing-username/missing-password/invalid-role/duplicate → 400; update role+name; update invalid role → 400; update password only; delete non-last; delete last → 400 |
| `StudentControllerTest`, `ResultControllerTest`, `LessonControllerTest`, etc. | CRUD and business logic coverage |