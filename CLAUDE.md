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