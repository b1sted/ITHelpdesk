# Autonomous Agent & Architectural Guidelines (AGENTS.md)

This document serves as the single source of truth for AI agents (SourceCraft, Cursor, Copilot) and software engineers working on the **IT Helpdesk System**. It defines system actors, the atomic state machine, the relational schema, API specifications, and code style conventions for both backend and frontend.

---

## 1. Project Domain & Context
* **Project Name:** IT Helpdesk System (Corporate Service Desk)
* **Domain:** Internal IT support and incident management for enterprise employees.
* **Core Philosophy:** Strictly atomic operations (one action = one transparent state change), layered architecture, zero entity leakage, and compile-time safety across the full stack.

---

## 2. Technical Stack & Tooling
* **Backend:**
   * **JDK:** Java 21 (LTS). Leverage records, pattern matching, and switch expressions.
   * **Framework:** Spring Boot 4.1+ (Spring Web, Spring Data JPA, Spring Security, Validation).
   * **Build Tool:** Gradle (Groovy DSL).
   * **Database:** PostgreSQL (with Hibernate ORM). Schema changes are managed exclusively by Flyway migrations.
   * **Boilerplate Reduction:** Immutable DTOs as Java `record`s; no Lombok — entities expose explicit accessors and beans rely on constructor injection.
   * **API Documentation:** Springdoc OpenAPI 3.1 (Springdoc 3.1.1, Swagger UI).
* **Frontend:**
   * **Core:** React 18, TypeScript, Vite.
   * **Styling:** Tailwind CSS (modern, utility-first UI).
   * **Icons:** Lucide React.
   * **HTTP Client:** Axios (configured with base URL `/api/v1` and auth interceptors).

---

## 3. System Actors (Operational Agents)

| Role Code | Display Name | Category | Scope & Responsibility |
|:---|:---|:---|:---|
| `ROLE_EMPLOYEE` | **Employee (Requester)** | Human | Submits technical issues, participates in ticket discussions, verifies solutions, and closes/reopens tickets. |
| `ROLE_AGENT` | **IT Specialist (Support Agent)** | Human | Monitors the pool of incoming issues, claims tickets (within a personal active-ticket limit), communicates with users, marks incidents as resolved, and can request release/reassignment with justification. |
| `ROLE_MANAGER` | **Support Department Manager** | Human | Monitors agent workload and SLA breaches, sets per-agent active-ticket and resolution-time limits, approves or rejects release/reassignment requests, extends ticket deadlines, and inspects all tickets read-only. |
| `ROLE_ADMIN` | **Administrator** | Human | Manages reference data (departments, problem categories), registers new employees, manages roles, limits and support settings, and monitors company-wide statistics. |
| `SYSTEM_DAEMON` | **System Scheduler** | Machine | Background runner for automated auto-closing of resolved tickets and SLA audits. |

---

## 4. The Atomic Business Processes (State Machine)

The ticket lifecycle is modeled as a deterministic finite-state machine. No composite or hidden transactions are permitted. The manager decision processes are recorded as separate immutable `ticket_requests` records so every approval or rejection remains transparent.

### SLA / Overdue Threshold (`app.ticket.overdue-hours`, env `TICKET_OVERDUE_HOURS`, default `24`):

A ticket is considered **overdue** when `now > due_at` and its status is one of `NEW`, `IN_PROGRESS`, `ANSWERED`. `WAITING_USER` pauses SLA tracking (the ball is in the requester's court), while `RESOLVED` and `CLOSED` are final states. For assigned tickets the time limit is taken from the assignee's `users.time_limit_hours`; unassigned tickets fall back to the global `app.ticket.overdue-hours` value. The deadline is derived as `due_at = created_at + limit-hours + extension_hours`. The API exposes derived `overdue` and `dueAt` fields in `TicketResponse`; the frontend highlights such tickets in red for `ROLE_AGENT`, `ROLE_MANAGER` and `ROLE_ADMIN` only.

### Automatic Closure (`app.ticket.auto-close-check-ms`, env `TICKET_AUTO_CLOSE_CHECK_MS`, default `3600000`):

`SYSTEM_DAEMON` periodically closes `RESOLVED` tickets whose `resolved_at` is older than `support_settings.auto_close_resolved_hours`; the value `0` disables auto-closing. Each auto-close sets `status = CLOSED` and `closed_at = now`.

### Valid Statuses (`TicketStatus` Enum):
* `NEW` — Created, waiting in the unassigned queue.
* `IN_PROGRESS` — Claimed by an IT specialist, currently being addressed.
* `WAITING_USER` — Paused by the IT specialist pending user response/clarification.
* `ANSWERED` — The author replied to a `WAITING_USER` ticket; awaiting the specialist again.
* `RESOLVED` — Marked as fixed by the IT specialist; awaiting employee verification.
* `CLOSED` — Final state; resolution confirmed by the author.

### State Transition & Authority Matrix:

| Process ID | Process Name | Current Status | Target Status | Authorized Actor | System Action & Side Effects |
|:---:|:---|:---|:---|:---|:---|
| **BP-1** | **Ticket Registration** | *None* | `NEW` | `EMPLOYEE` | Creates ticket record; `assignee` is set to `null`; sets `created_at` and initializes `extension_hours = 0`. |
| **BP-2** | **Claim Ticket** | `NEW` | `IN_PROGRESS` | `AGENT` | Sets `assignee_id = authenticated_agent_id`; moves status to `IN_PROGRESS`. Rejected with `409 Conflict` when the agent already has `ticket_limit` active tickets (`IN_PROGRESS`, `WAITING_USER`, `ANSWERED`). |
| **BP-3** | **Add Message** | *Any active* | *Unchanged* (or `ANSWERED`) | `EMPLOYEE`, `AGENT` (Assignee) | Appends a new immutable message to `messages` linked to the ticket. An agent may post only after claiming the ticket (must be its assignee) and can always read the thread. When the **author** of a `WAITING_USER` ticket sends a message, the status automatically becomes `ANSWERED`. |
| **BP-4** | **Pause Ticket** | `IN_PROGRESS` | `WAITING_USER` | `AGENT` (Assignee) | Toggles status to indicate pending user action. |
| **BP-5** | **Resolve Ticket** | `IN_PROGRESS`, `WAITING_USER`, `ANSWERED` | `RESOLVED` | `AGENT` (Assignee) | Sets status to `RESOLVED`, records `resolved_at`, clears `closed_at`; locks specialist editing actions. |
| **BP-6** | **Confirm & Close** | `RESOLVED` | `CLOSED` | `EMPLOYEE` (Author) | Sets status to `CLOSED`, records `closed_at`; ticket moves to archive. |
| **BP-7** | **Reopen Ticket** | `RESOLVED` | `IN_PROGRESS` | `EMPLOYEE` (Author) | Reverts status to `IN_PROGRESS`, clears `resolved_at` and `closed_at`; requires a reason (`reason` field of `TicketStatusChangeRequest`). |
| **BP-8** | **Resume Work** | `ANSWERED` | `IN_PROGRESS` | `AGENT` (Assignee) | The specialist continues working after the author's reply. |
| **BP-9** | **Request Release** | *Any active assigned* | *Unchanged* | `AGENT` (Assignee) | Creates a `PENDING` `ticket_requests` record of type `RELEASE` with a mandatory `reason`. No ticket mutation happens until the manager decides. |
| **BP-10** | **Request Reassignment** | *Any active assigned* | *Unchanged* | `AGENT` (Assignee) | Creates a `PENDING` `ticket_requests` record of type `REASSIGN` with a mandatory `reason` and an optional `target_assignee_id` pointing to another `AGENT` (the manager may choose instead). No ticket mutation happens until the manager decides. |
| **BP-11** | **Decide Ticket Request** | *Pending request* | *APPROVED / REJECTED* | `MANAGER`, `ADMIN` | Approving `RELEASE` clears `assignee_id` and sets `NEW`. Approving `REASSIGN` assigns the target agent (chosen by the requester or the manager) and sets `IN_PROGRESS` (target capacity is re-checked). Rejecting leaves the ticket unchanged. Every decision stores `decided_by_id`, `decided_at` and an optional `decision_comment`. |
| **BP-12** | **Extend Deadline** | *Any active assigned* | *Unchanged* | `AGENT` (Assignee), `MANAGER`, `ADMIN` | Adds an immutable `ticket_deadline_extensions` row with the requested hours (1–720 in total) and a mandatory `reason`, then increments `tickets.extension_hours`. The deadline becomes `created_at + limit-hours + extension_hours`. |
| **BP-13** | **Auto-Close Resolved** | `RESOLVED` | `CLOSED` | `SYSTEM_DAEMON` | When `support_settings.auto_close_resolved_hours > 0` and `resolved_at` is older than the configured window, sets `status = CLOSED` and `closed_at = now`. |

---

## 5. Relational Database Schema (8 Entities)

All tables must be normalized (3NF). Class names must be **singular**, and table names must be **plural**.

```text
 [ departments ] 1 ─── N [ users ] 1 ─── N [ messages ]
                             │                 │
                             │ (creator /      │
                             │  assignee)      │
                             ▼                 ▼
 [ categories ]  1 ──────── N [ tickets ] 1 ───┘
                                  │
                                  ├── 1 ─── N [ ticket_requests ]
                                  └── 1 ─── N [ ticket_deadline_extensions ]

 [ support_settings ] — singleton row with automation settings
```

### Table Definitions:

1. **`departments`**
   * `id`: `BIGINT` (Primary Key, Identity)
   * `name`: `VARCHAR(100)` (Not Null, Unique)

2. **`users`**
   * `id`: `BIGINT` (Primary Key, Identity)
   * `full_name`: `VARCHAR(150)` (Not Null)
   * `email`: `VARCHAR(100)` (Not Null, unique case-insensitive index on `lower(email)`)
   * `password`: `VARCHAR(255)` (Not Null, BCrypt hashed)
   * `role`: `VARCHAR(20)` (Not Null, Enum: `EMPLOYEE`, `AGENT`, `MANAGER`, `ADMIN`)
   * `ticket_limit`: `INTEGER` (Not Null, Default 10, meaningful for `AGENT`; maximum active tickets)
   * `time_limit_hours`: `INTEGER` (Not Null, Default 24, meaningful for `AGENT`; maximum hours to resolve an assigned ticket)
   * `department_id`: `BIGINT` (Foreign Key -> `departments.id`, Not Null)

3. **`categories`**
   * `id`: `BIGINT` (Primary Key, Identity)
   * `name`: `VARCHAR(100)` (Not Null)

4. **`tickets`**
   * `id`: `BIGINT` (Primary Key, Identity)
   * `title`: `VARCHAR(200)` (Not Null)
   * `description`: `TEXT` (Not Null)
   * `status`: `VARCHAR(30)` (Not Null, Enum)
   * `created_at`: `TIMESTAMP` (Not Null)
   * `resolved_at`: `TIMESTAMP` (Nullable, set when the ticket becomes `RESOLVED`)
   * `closed_at`: `TIMESTAMP` (Nullable, set when the ticket becomes `CLOSED`)
   * `extension_hours`: `INTEGER` (Not Null, default 0, cumulative deadline extension)
   * `version`: `BIGINT` (Not Null, default 0, optimistic locking)
   * `creator_id`: `BIGINT` (Foreign Key -> `users.id`, Not Null)
   * `assignee_id`: `BIGINT` (Foreign Key -> `users.id`, Nullable)
   * `category_id`: `BIGINT` (Foreign Key -> `categories.id`, Not Null)
   * Derived only (not stored): `due_at = created_at + limit-hours + extension_hours`, `overdue = status in (NEW, IN_PROGRESS, ANSWERED) && now > due_at`.

5. **`messages`**
   * `id`: `BIGINT` (Primary Key, Identity)
   * `text`: `TEXT` (Not Null)
   * `created_at`: `TIMESTAMP` (Not Null)
   * `ticket_id`: `BIGINT` (Foreign Key -> `tickets.id`, Not Null)
   * `author_id`: `BIGINT` (Foreign Key -> `users.id`, Not Null)

6. **`ticket_requests`**
   * `id`: `BIGINT` (Primary Key, Identity)
   * `type`: `VARCHAR(20)` (Not Null, Enum: `RELEASE`, `REASSIGN`)
   * `status`: `VARCHAR(20)` (Not Null, Enum: `PENDING`, `APPROVED`, `REJECTED`)
   * `reason`: `TEXT` (Not Null, mandatory justification from the agent)
   * `decision_comment`: `VARCHAR(1000)` (Nullable, manager comment)
   * `created_at`: `TIMESTAMP` (Not Null)
   * `decided_at`: `TIMESTAMP` (Nullable)
   * `ticket_id`: `BIGINT` (Foreign Key -> `tickets.id`, Not Null)
   * `requester_id`: `BIGINT` (Foreign Key -> `users.id`, Not Null)
   * `target_assignee_id`: `BIGINT` (Foreign Key -> `users.id`, Nullable, optional for `REASSIGN`)
   * `decided_by_id`: `BIGINT` (Foreign Key -> `users.id`, Nullable)
   * `version`: `BIGINT` (Not Null, default 0, optimistic locking)

7. **`ticket_deadline_extensions`**
   * `id`: `BIGINT` (Primary Key, Identity)
   * `hours`: `INTEGER` (Not Null, 1–720 per action)
   * `reason`: `TEXT` (Not Null, mandatory justification)
   * `created_at`: `TIMESTAMP` (Not Null)
   * `ticket_id`: `BIGINT` (Foreign Key -> `tickets.id`, Not Null)
   * `created_by_id`: `BIGINT` (Foreign Key -> `users.id`, Not Null)

8. **`support_settings`**
   * `id`: `BIGINT` (Primary Key, Identity)
   * `auto_close_resolved_hours`: `INTEGER` (Not Null, default 0; 0 disables auto-closing)

---

## 6. Backend Architecture Guidelines

### 6.1. Modular Feature Packaging Blueprint ("One Package - One Entity")
Each domain module must strictly replicate the enterprise package anatomy established in the project.

For any entity `<Entity>` (e.g., `Department`, `User`, `Category`, `Ticket`, `Message`), create the following internal subpackages:

```text
ru.basted.ithelpdesk.<feature>/
├── controller/
│   ├── <Entity>API.java             <-- Interface with OpenAPI / Swagger annotations
│   └── <Entity>Controller.java      <-- Clean @RestController implementing <Entity>API
├── dto/
│   ├── <Entity>CreateRequest.java   <-- Java Record for creation payload
│   ├── <Entity>ChangeName.java      <-- Java Record for partial/name update payload
│   └── <Entity>Response.java        <-- Java Record for response payload
├── entity/
│   └── <Entity>.java                <-- JPA @Entity
├── mapper/
│   └── <Entity>Mapper.java          <-- MapStruct mapping interface (Entity <-> DTO)
├── repository/
│   └── <Entity>Repository.java      <-- Spring Data JpaRepository
└── service/
    ├── <Entity>Service.java         <-- Business logic interface
    └── <Entity>ServiceImpl.java     <-- @Service implementation class (@Transactional)
```

**Instruction for Agents:** When prompted to implement any new domain entity, generate all 6 subpackages and their corresponding classes/interfaces strictly following this blueprint.

### 6.2. DTO & Model Rules
* **No Entity Exposure:** Never return `@Entity` classes directly from Controller endpoints.
* **Java Records:** All DTOs must be implemented as immutable Java `record`s.
* **No `@JsonView`:** Do not use Jackson `@JsonView`. Create separate, explicit Request and Response DTOs.
* **Asymmetric Mapping:** Request DTOs accept entity IDs (`Long categoryId`), Response DTOs provide human-readable fields (`String categoryName`) or nested records.

### 6.3. JPA & Hibernate Conventions
* **Primary Key Strategy:** Always use `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
* **Fetch Type:** Always explicitly declare `fetch = FetchType.LAZY` on `@ManyToOne` relationships.
* **Foreign Key Naming:** Explicitly name foreign key constraints using `@ForeignKey(name = "fk_<table>_<column>")`.
* **Transaction Management:** Annotate all state-changing service methods with `@Transactional`.

### 6.4. Security & RBAC Conventions
* Passwords must be encoded using `BCryptPasswordEncoder`.
* Protect endpoints using declarative method security: `@PreAuthorize("hasRole('ADMIN')")`.
* Ensure ownership checks: Only the creator of a ticket can confirm closing or reopening it.
* **JWT:** Tokens are signed with HS256 using a secret of at least 32 bytes (from `app.jwt.secret`, overridable via `JWT_SECRET`). Tokens carry `sub` (email), `uid` and `role` claims with a 24-hour expiry; every request re-validates that the user still exists and derives granted authorities from the current database role (so role changes apply immediately), plus validates the token issuer.
* **Email:** addresses are stored normalized (trim + lower-case) on create and update; a unique index on `lower(email)` enforces case-insensitive uniqueness and login remains case-insensitive.
* **Login rate limit:** Failed login attempts per email are limited (`app.rate-limit.max-attempts`, default 5 per 5 minutes) and respond `429 Too Many Requests`; expired windows are removed every `app.rate-limit.cleanup-ms` (env `RATE_LIMIT_CLEANUP_MS`, default 1 minute).
* **CORS:** The frontend origin(s) are configured via `app.cors.allowed-origins` (default `http://localhost:5173`).
* **Manager authority:** `ROLE_MANAGER` (displayed as **Руководитель**) and `ROLE_ADMIN` can read agent statistics, list and decide ticket requests, list support agents, change an agent's `ticket_limit` and `time_limit_hours`, extend ticket deadlines and manage support settings. Managers cannot claim tickets, change ticket statuses, or post messages.
* **Agent capacity:** `AgentCapacityPolicy` enforces the per-agent active-ticket limit on claim and on approved reassignment. Counted statuses are `IN_PROGRESS`, `WAITING_USER`, `ANSWERED`.
* **SLA:** each agent has a persisted non-null `time_limit_hours` limit; `app.ticket.overdue-hours` (env `TICKET_OVERDUE_HOURS`) is the fallback for unassigned tickets only. `WAITING_USER` tickets are excluded from overdue detection.
* **Deadline extension:** the assigned agent, a manager or an administrator can extend an active ticket by 1–720 hours in total with a mandatory reason. Every action is stored in `ticket_deadline_extensions` and increments `tickets.extension_hours`.
* **Auto-closing:** `SYSTEM_DAEMON` runs every `app.ticket.auto-close-check-ms` (default 1 hour) and closes `RESOLVED` tickets older than `support_settings.auto_close_resolved_hours`; `0` disables it.
* **Concurrency:** `tickets` and `ticket_requests` carry a `version` column with JPA `@Version`. Taking a ticket is an atomic `UPDATE ... WHERE status = NEW` that also re-checks the agent capacity; a stale optimistic update is mapped to `409 Conflict` with a Russian message.

### 6.5. OpenAPI (Swagger) Documentation, Validation & Localization Rules
The system communicates with users and developers **exclusively in Russian**.

* **Code artifacts:** Classes, methods, database tables, and URL paths must remain in English.
* **User-facing texts:** All Swagger annotations (`@Operation`), `@Schema`, validation messages (`message = "..."`), and API error messages **MUST BE STRICTLY IN RUSSIAN**.
* **OpenAPI on Interfaces:** Place all `@Tag`, `@Operation`, and `@ApiResponses` annotations exclusively on `<Entity>API` interfaces.
* **Jakarta Constraints:** Every Request DTO field must have constraints with informative Russian messages (`@NotBlank`, `@Size`, `@NotNull`, `@Email`). Controllers must enforce `@Valid`.

### 6.6. Error Handling & Utilities
* **NotFound:** Missing entities must throw `core.exception.NotFoundException` (maps to `404 Not Found`).
* **Conflict:** Duplicate or invalid state transitions must throw `core.exception.ConflictException` (maps to `409 Conflict`).
* **Bad Request:** Invalid arguments and validation failures map to `400 Bad Request` via the global `core.exception.ApiExceptionHandler`.
* **Resource Masking:** Unauthenticated requests, invalid tokens and insufficient-role calls all map to `404 Not Found` with the generic `{"message": "Ресурс не найден"}` body (see section 7.1).
* **Current User:** The authenticated principal is resolved through the `core.security.CurrentUser` component; ownership checks (ticket author, assignee) must be enforced in the service layer.
* **No Lombok:** Entities expose explicit accessors and beans rely on constructor injection.

---

## 7. REST API Endpoints Specification

Base Path: `/api/v1`

### 7.1. Authentication
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `POST` | `/auth/login` | Authenticate and obtain a JWT token | Public | `200 OK`, `404 Not Found` |

All authenticated endpoints expect the JWT token in the `Authorization: Bearer <token>` header. The token is issued with a 24-hour validity and encodes the `role` claim; `hasRole(...)` checks use authorities derived from the current database role, so role changes apply immediately. Public paths always include `/auth/login` and the Swagger UI paths.

**Resource Masking:** All API paths (except `/auth/login` and Swagger paths) are protected. When a request is unauthenticated, carries an invalid token, or lacks the required role, the server responds with `404 Not Found` and a generic `{"message": "Ресурс не найден"}` body — never `401` or `403` — so that the existence of resources is not disclosed. Authorized clients receive normal statuses (`200`, `201`, `204`, `400`, `404` for validation/missing, `409` for conflicts).

### 7.2. Tickets (Core Domain Workflows)
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `POST` | `/tickets` | Register a new ticket (BP-1) | `EMPLOYEE` | `201 Created`, `400 Bad Request` |
| `GET` | `/tickets` | List tickets (with filters by status/role). `EMPLOYEE` receives only own tickets; `AGENT` receives tickets assigned to them plus unassigned `NEW` tickets; `MANAGER`/`ADMIN` receive all | Authenticated | `200 OK` |
| `GET` | `/tickets/{id}` | Get full ticket details with messages. `EMPLOYEE` may read only own tickets; `AGENT`/`MANAGER`/`ADMIN` may read any ticket | Authenticated | `200 OK`, `404 Not Found` |
| `PATCH`| `/tickets/{id}/assign` | Claim ticket into work (BP-2) | `AGENT` | `200 OK`, `409 Conflict` (status or personal limit) |
| `PATCH`| `/tickets/{id}/status` | Transition status (BP-4–8) | `EMPLOYEE`, `AGENT` | `200 OK`, `400 Bad Request`, `409 Conflict` |
| `PATCH`| `/tickets/{id}/deadline` | Extend the ticket deadline with a mandatory reason (BP-12) | `AGENT` (Assignee), `MANAGER`, `ADMIN` | `200 OK`, `400 Bad Request`, `409 Conflict` |

### 7.3. Messages
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `POST` | `/tickets/{id}/messages`| Post a message in the ticket thread (BP-3)| `EMPLOYEE` (author), `AGENT` (assignee) | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `GET` | `/tickets/{id}/messages` | List messages of the ticket thread in chronological order | Authenticated | `200 OK`, `404 Not Found` |

An agent can always read the thread but may post only when the ticket is assigned to that agent (BP-2 completed). An employee may post only in their own tickets. Administrators can read every ticket and thread but cannot post messages. Otherwise the API returns `409 Conflict`.

### 7.4. Categories Management (Admin CRUD)
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `GET` | `/categories` | Fetch all incident categories | Authenticated | `200 OK` |
| `GET` | `/categories/{id}` | Get single category by ID | Authenticated | `200 OK`, `404 Not Found` |
| `POST` | `/categories` | Create a new category | `ADMIN` | `201 Created`, `409 Conflict` |
| `PUT` | `/categories/{id}` | Update existing category name | `ADMIN` | `200 OK`, `404 Not Found` |
| `DELETE`| `/categories/{id}` | Delete category by ID | `ADMIN` | `204 No Content`, `404 Not Found` |

### 7.5. Departments Management (Admin CRUD)
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `GET` | `/departments` | Fetch all company departments | Authenticated | `200 OK` |
| `GET` | `/departments/{id}` | Get department by ID | Authenticated | `200 OK`, `404 Not Found` |
| `POST` | `/departments` | Create a new department | `ADMIN` | `201 Created`, `409 Conflict` |
| `PUT` | `/departments/{id}` | Update existing department name | `ADMIN` | `200 OK`, `404 Not Found` |
| `DELETE`| `/departments/{id}` | Delete department by ID | `ADMIN` | `204 No Content`, `404 Not Found` |

### 7.6. Users Management (Admin CRUD & Directory)
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `GET` | `/users` | List all users (directory) | `ADMIN` | `200 OK` |
| `GET` | `/users/agents` | List support specialists (for reassignment) | `AGENT`, `MANAGER`, `ADMIN` | `200 OK` |
| `GET` | `/users/{id}` | Get user profile by ID | `ADMIN`, Self | `200 OK`, `404 Not Found` |
| `POST` | `/users` | Provision a new user account | `ADMIN` | `201 Created`, `409 Conflict` |
| `PATCH`| `/users/{id}` | Partially update user profile / department / role | `ADMIN` | `200 OK`, `404 Not Found` |
| `PATCH`| `/users/{id}/limits` | Set the active-ticket and resolution-time limits of an `AGENT` | `MANAGER`, `ADMIN` | `200 OK`, `409 Conflict` |
| `DELETE`| `/users/{id}` | Deactivate or remove user | `ADMIN` | `204 No Content`, `404 Not Found` |

### 7.7. Support Statistics (Manager & Admin)
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `GET` | `/statistics/agents` | Per-agent workload: limit, total, active, resolved, closed and overdue ticket counts | `MANAGER`, `ADMIN` | `200 OK` |
| `GET` | `/statistics/period` | Created, resolved and closed ticket counts (aggregated in the database) by month (from the first activity month through the current month) or by year (from the first activity year, padded to at least five years with future years), plus average resolution time. Optional `granularity=MONTH\|YEAR` | `MANAGER`, `ADMIN` | `200 OK` |

### 7.8. Ticket Requests (Release / Reassignment)
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `POST` | `/tickets/{id}/requests` | Create a `RELEASE` or `REASSIGN` request with mandatory reason (BP-9, BP-10) | `AGENT` (Assignee) | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `GET` | `/tickets/{id}/requests` | List the request history of one ticket | `AGENT`, `MANAGER`, `ADMIN` | `200 OK`, `404 Not Found` |
| `GET` | `/requests` | List all requests, optionally filtered by `status` (`PENDING`, `APPROVED`, `REJECTED`); without a filter returns the full history | `MANAGER`, `ADMIN` | `200 OK` |
| `PATCH`| `/requests/{id}/decision` | Approve or reject a request, optionally with a comment (BP-11) | `MANAGER`, `ADMIN` | `200 OK`, `400 Bad Request`, `409 Conflict` |

Only one `PENDING` request per ticket is allowed. An approved `RELEASE` returns the ticket to `NEW` with no assignee; an approved `REASSIGN` transfers it to the chosen agent and re-checks that agent's capacity.

### 7.9. Support Settings (Manager & Admin)
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `GET` | `/settings/support` | Read the auto-close window for `RESOLVED` tickets | `MANAGER`, `ADMIN` | `200 OK` |
| `PUT` | `/settings/support` | Update `autoCloseResolvedHours` (0 disables auto-closing) | `MANAGER`, `ADMIN` | `200 OK`, `400 Bad Request` |

---

## 8. Frontend Architecture & React Guidelines

The frontend is a modern React SPA located in `/frontend`, consuming the Spring Boot REST API.

### 8.1. Directory Structure
```text
frontend/
├── src/
│   ├── api/                      <-- Axios client and modular API functions
│   │   ├── client.ts             (baseURL: '/api/v1', interceptors, profile storage)
│   │   ├── authApi.ts
│   │   ├── adminApi.ts           (users, departments, categories, agent limits)
│   │   ├── errorMessage.ts
│   │   ├── ticketsApi.ts
│   │   ├── ticketRequestsApi.ts
│   │   └── statisticsApi.ts      (agent/period statistics and support settings)
│   ├── types/                    <-- TypeScript interfaces strictly mirroring Java DTOs
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   └── ticket.ts
│   ├── components/
│   │   ├── Navbar.tsx            (Top header with tickets, statistics, admin links, User info and Logout)
│   │   ├── StatusBadge.tsx       (Color-coded status pills: NEW, IN_PROGRESS, etc.)
│   │   ├── MessageThread.tsx     (Ticket conversation timeline + message submission form)
│   │   ├── TicketRequestList.tsx (Release/reassignment request history and manager decision controls)
│   │   └── *Route.tsx            (ProtectedRoute, EmployeeRoute, ManagerRoute, AdminRoute guards)
│   ├── context/
│   │   └── AuthContext.tsx       (Token/profile state, login, logout and profile refresh)
│   ├── pages/
│   │   ├── LoginPage.tsx         (Screen 1: Authentication form)
│   │   ├── TicketListPage.tsx    (Screen 2: Table with role-aware filter tabs)
│   │   ├── CreateTicketPage.tsx  (Screen 3: Ticket submission form)
│   │   ├── TicketDetailPage.tsx  (Screen 4: Adaptive detail view for 4a/4b/4c/4d)
│   │   ├── StatisticsPage.tsx    (Screen 6: Agent workload, period dynamics, settings and requests)
│   │   └── AdminPage.tsx         (Screen 5: Admin console for users, departments and categories)
│   ├── utils/
│   │   └── roleLabels.ts         (Russian labels for all roles)
│   ├── App.tsx                   (Router and Role Context)
│   └── main.tsx
├── package.json
└── vite.config.ts
```

### 8.2. UI Screens & Adaptive Action Panel Rules
1. **Screen 1 (Login):** Email and password input, stores authenticated token & user profile (`role`, `fullName`) in client state.
2. **Screen 2 (Ticket List):** Table with columns: Number, Title, Category, Assignee, Status (color-coded badge), Created Date. `EMPLOYEE` sees only own tickets with `[Активные]` / `[Архив]` filters; `AGENT` sees assigned tickets and the unassigned `NEW` queue with the full status filter set; `MANAGER` and `ADMIN` see all tickets with the full status filter set. Overdue rows are highlighted in red and carry a `[Просрочена]` badge for `AGENT`, `MANAGER` and `ADMIN` only. Displays `[+ Создать заявку]` button only for `ROLE_EMPLOYEE`.
3. **Screen 3 (Create Ticket Form):** Category dropdown, Title input, Description textarea. Validates required fields before submitting to `POST /api/v1/tickets`.
4. **Screen 4 (Ticket Details - Adaptive 4a / 4b / 4c / 4d View):**
   * Left section: Ticket metadata (including `resolvedAt` / `closedAt` when present), full description, and chronological `MessageThread`.
   * Conversation rules: an `AGENT` sees the whole thread but the submission form is hidden until the agent is the ticket assignee (a hint explains why); an `EMPLOYEE` sees the form only in their own ticket while it is not `CLOSED` and cannot access foreign tickets; an `ADMIN` and a `MANAGER` see the whole thread in read-only mode.
   * Action panel adapts dynamically based on authenticated role and current ticket status:
      * **Agent View (4a):** Shows `[Взять в работу]` if `status === 'NEW'`. Shows `[Жду ответа]` and `[Решено]` if `status === 'IN_PROGRESS'`, and `[Продолжить работу]` / `[Решено]` if `status === 'ANSWERED'`. Action buttons are hidden if `status === 'RESOLVED'`.
      * **Author View (4b):** Action buttons are hidden while `NEW` or `IN_PROGRESS`. Shows `[Закрыть заявку]` and `[Вернуть на доработку]` ONLY when `status === 'RESOLVED'`.
      * **Admin View (4c):** Read-only inspection of any ticket. No assign, status or message submission actions are shown; administrators can still decide pending requests and see overdue markers in the list.
      * **Manager View (4d):** Read-only ticket and thread inspection plus a request panel with `[Одобрить]` / `[Отклонить]` for pending requests. Managers never see message submission, assign or status controls.
   * An assigned `AGENT` on an active ticket sees a request panel to `[Отказаться от заявки]` or `[Переназначить специалисту]`. The agent must provide a mandatory justification and, for reassignment, choose another agent. While a `PENDING` request exists the form is replaced with a waiting notice.
   * An assigned `AGENT`, a `MANAGER` or an `ADMIN` can open the deadline extension dialog, enter 1–720 hours and a mandatory reason; the ticket then shows the extended `dueAt` and the full extension history.
5. **Screen 5 (Admin Console):** Accessible at `/admin` only to `ROLE_ADMIN`. Provides tabs for users, departments and categories, plus a shortcut back to the ticket list. Users can be created, edited (full name, email, department, role) and deleted; roles include `EMPLOYEE`, `AGENT`, `MANAGER` and `ADMIN`.
6. **Screen 6 (Statistics):** Accessible at `/statistics` to `ROLE_MANAGER` and `ROLE_ADMIN`. Shows per-agent ticket and time limits, total/active/resolved/closed/overdue counts, inline limit editing, created/resolved/closed dynamics by month or year with average resolution time, the auto-close settings form, and the release/reassignment request queue with a pending/all-history filter and decision controls.

### 8.3. Type Safety (DTO Mirroring)
Every TypeScript type in `src/types/` must mirror its corresponding Java DTO Record. Example:
```typescript
export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_USER' | 'ANSWERED' | 'RESOLVED' | 'CLOSED';

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  departmentName: string;
}

export interface MessageResponse {
  id: number;
  text: string;
  createdAt: string;
  ticketId: number;
  author: UserSummary;
}

export interface TicketDeadlineExtensionResponse {
  id: number;
  hours: number;
  reason: string;
  createdAt: string;
  createdBy: UserSummary;
}

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  createdAt: string;
  creator: UserSummary;
  assignee: UserSummary | null;
  categoryName: string;
  overdue: boolean;
  dueAt: string;
  resolvedAt: string | null;
  closedAt: string | null;
  extensionHours: number;
  deadlineExtensions: TicketDeadlineExtensionResponse[];
  messages: MessageResponse[];
}
```

---

## 9. Build & Verification Commands
* **Start local PostgreSQL:** `docker compose up -d`
* **Run backend application:** `./gradlew bootRun` (serves REST API and Swagger UI at `http://localhost:8080/swagger-ui.html`)
* **Database schema & reference data:** Flyway owns the schema (`src/main/resources/db/migration`, baseline `V1`), including the `IT Service Desk` department, default incident categories, the singleton `support_settings` row, constraints, indexes and NOT NULL defaults. `spring.jpa.hibernate.ddl-auto=validate`. On startup `core.config.DBInitializer` only idempotently creates the admin account `admin@company.ru` / `Admin2024!` (password overridable via `ADMIN_PASSWORD`, full name via `ADMIN_FULL_NAME`). No backfills or data repair run in the application.
* **Overdue threshold:** `TICKET_OVERDUE_HOURS` (default `24`, property `app.ticket.overdue-hours`) is the fallback when an assigned agent has no personal time limit; managers can set per-agent limits in the statistics screen.
* **Auto-close scheduler:** `TICKET_AUTO_CLOSE_CHECK_MS` (default `3600000`, property `app.ticket.auto-close-check-ms`) controls how often `SYSTEM_DAEMON` checks for `RESOLVED` tickets to close.
* **Run test suite:** `./gradlew test`
* **Build backend archive:** `./gradlew build -x test`
* **Run frontend dev server:** `cd frontend && npm run dev`
* **Build frontend for production:** `cd frontend && npm run build`
* **Run the production stack:** `docker compose --env-file .env -f docker-compose.prod.yml up -d --build` (nginx serves the SPA and proxies `/api`, Swagger UI and OpenAPI docs to the backend).
* **SPA cache policy:** the production nginx serves `index.html` with `Cache-Control: no-store` and hashed `/assets/*` files with a 30-day immutable cache, so a rebuilt frontend (including the statistics screen) is picked up on the next navigation instead of a stale cached bundle.

---

## 10. Development Workflow (Git & Verification)

These rules apply to every task unless the user explicitly says otherwise.

### 10.1. Branches and Commits
* Before starting, `git status` must be clean; if it is not, stop and report it.
* One topic = one branch, created from an up-to-date `main`. Never mix unrelated changes in one branch. Match the naming of existing branches and the message style of existing commits (check `git branch -a` and `git log --oneline -30`).
* Inside a branch, make meaningful commits split by logical step (for example: schema/migration, service logic, error handling, tests, documentation). No single giant commit, no meaningless "wip"/"fix" commits. Every commit must build and pass the tests.
* Merge into `main` only with `git merge --no-ff <branch>` and a meaningful merge commit message in the style of the existing history. Do not delete the branch after merging.
* Without explicit user permission do not run `git push`, `--force`, `reset --hard`, rebase or squash on existing history.
* On a merge conflict do not resolve it blindly: stop and show the conflict.

### 10.2. Verification Before Committing and Merging
* **Backend:** `./gradlew test` and `./gradlew build -x test` must pass. If API behavior changed, start the application (section 9) and call the affected endpoints for real: both the success path and the error path (status code and Russian message).
* **Frontend:** run the lint, type-check, test (if present) and build commands from `frontend/package.json`. If DTOs, response codes, error texts or the API contract changed, update frontend types and code (section 8.3) and check the affected screens against the local backend.
* If the UI cannot be checked visually, do not claim it was verified: state that it was checked by build, types and API calls, and list what the user should check manually.
* If a check fails, do not commit or merge: fix the cause, or stop and explain.

### 10.3. Documentation
* When a change affects the schema, API, processes, configuration properties or build commands, update the corresponding sections of this file (4, 5, 7, 9) in the same branch.

### 10.4. Final Report
At the end of a task list: branches, commits, what exactly was verified and how, and the merge result.