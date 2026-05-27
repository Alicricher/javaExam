# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Telegram-based dentistry education platform for Uzbek-speaking students. Two separate Spring Boot bots share a common library:
- **studentbot** — student-facing bot (Uzbek UI via `UzMessages`)
- **adminbot** — admin-facing bot for content management, student oversight, and situational answer grading
- **shared** — common models, repositories, services, state machine, and Flyway migrations

## Build & Run

```bash
# Build all modules
mvn package -DskipTests

# Build only studentbot and its dependencies
mvn package -pl shared,studentbot -am -DskipTests

# Build only adminbot and its dependencies
mvn package -pl shared,adminbot -am -DskipTests

# Run everything with Docker (requires .env file from .env.example)
cp .env.example .env   # then fill in values
docker compose up --build

# Run a single module's tests (no tests currently exist, but the pattern)
mvn test -pl studentbot
```

There is no local dev server profile; run via Docker or directly from the compiled jar with the required environment variables set.

## Architecture

### Module Structure

```
shared/          ← library jar (no Spring Boot main class)
  model/         ← plain POJOs (Student, Lesson, Test, Question, SituationalTask, etc.)
  repository/    ← JDBC-based repositories (no JPA/ORM)
  service/       ← StateManager, GradingService, ImportService, NotificationService, FileService, TestService
  state/         ← StateConstants + state data POJOs serialized to JSONB
  config/        ← AppProperties (@ConfigurationProperties prefix="app")

studentbot/      ← Spring Boot app
  bot/           ← StudentBot (LongPollingUpdateConsumer) + StudentUpdateDispatcher
  handler/       ← RegistrationHandler, ProfileHandler, LessonHandler, TestHandler, TheoryHandler, SituationalHandler
  keyboard/      ← StudentKeyboards (callback prefix constants + InlineKeyboardMarkup builders)
  localization/  ← UzMessages (all UI strings as constants)
  scheduler/     ← TimeoutEnforcer (@Scheduled, runs every 60s)

adminbot/        ← Spring Boot app
  bot/           ← AdminBot + AdminUpdateDispatcher
  handler/       ← AuthHandler, StudentsHandler, ResultsHandler, ManagementHandler
  keyboard/      ← AdminKeyboards
  localization/  ← AdminMessages
```

### State Machine

All FSM state is persisted in the `user_states` table (`telegram_id`, `bot_type`, `state`, `state_data` JSONB). `StateManager` wraps reads/writes; `state_data` is serialized/deserialized via Jackson to typed POJOs (e.g., `TestStateData`, `SituationalStateData`, `LessonMenuStateData`). State constants live in `StateConstants`. `bot_type` is `"student"` or `"admin"`, so both bots share the same table without conflict.

### Dispatcher / Concurrency Model

Each bot has an `UpdateDispatcher` with a fixed `ThreadPoolExecutor` and a `ConcurrentHashMap<Long, ReentrantLock>`. Every incoming Telegram update is submitted to the pool; the per-user lock serializes concurrent messages from the same user. Idle locks are periodically cleaned up via `cleanupLocks(ttlMs)` (called by a `@Scheduled` method). The pool uses `DiscardPolicy` — updates are dropped silently if the queue is full.

### Callback Routing

Callback data uses string prefixes defined as constants on the `Keyboards` classes (e.g., `CB_UNIT`, `CB_TEST`, `CB_LESSON`). The dispatcher does `data.startsWith(...)` matching. Adding a new callback requires: (1) a prefix constant on the keyboards class, (2) a branch in the dispatcher's `handleCallback`, (3) a handler method.

### Grading Pipeline

`GradingService` calls any OpenAI-compatible chat completions API. The system prompt is loaded from `configs/grading_prompt.txt`. Per-lesson supplementary prompts can be placed in `configs/grading/lesson_<id>.txt` and are appended to the system prompt automatically. The response must be `{"grade": 0-100, "feedback": "...", "passed": true|false}`. Passing threshold: `grade >= 60` (also `StateConstants.MANUAL_GRADE_PASS_THRESHOLD`).

### Cross-Bot Notification

`NotificationService` (in shared, instantiated in adminbot) holds a reference to the **student** bot's `TelegramClient`. When an admin grades a situational answer, it sends a direct message to the student via the student bot client.

### Database & Migrations

Raw JDBC via Spring's `JdbcTemplate`. Flyway migrations live in `shared/src/main/resources/db/migration/` (V1–V8). Both bots run the same migrations on startup (idempotent). Schema: `students`, `admins`, `units`, `lessons`, `tests`, `questions`, `answer_options`, `test_results`, `test_answers`, `theory_materials`, `situational_tasks`, `situational_answers`, `test_retakes`, `situational_retakes`, `user_states`.

### Import Format

Admin can upload `.xlsx` or `.csv` to bulk-import test questions. Column order: `Question | Points | Option A | Option B | Option C | Option D | [Option E] | Correct Answer (letter)`. A download template button generates an example file.

## Configuration

All config is under the `app.*` prefix (`AppProperties`). Key env vars (see `.env.example`):

| Env var | Property | Purpose |
|---|---|---|
| `STUDENT_BOT_TOKEN` | `app.student-bot-token` | Student bot Telegram token |
| `ADMIN_BOT_TOKEN` | `app.admin-bot-token` | Admin bot Telegram token |
| `ADMIN_PASSWORD` | `app.admin-password` | Admin login password (bcrypt-hashed at runtime) |
| `APP_MATERIALS_PATH` | `app.materials-path` | File storage root for theory materials |
| `GRADING_API_URL` | `app.grading.api-url` | OpenAI-compatible base URL |
| `GRADING_API_KEY` | `app.grading.api-key` | API key |
| `GRADING_MODEL` | `app.grading.model` | Model name (default: `gpt-4o`) |

Dispatcher tuning: `app.dispatcher.workers` (default 64), `app.dispatcher.queue-size` (default 4096), `app.dispatcher.user-ttl-ms` (default 300000).
