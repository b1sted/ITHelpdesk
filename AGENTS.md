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
  * **Framework:** Spring Boot 3.4+ (Spring Web, Spring Data JPA, Spring Security, Validation).
  * **Build Tool:** Gradle (Groovy DSL).
  * **Database:** PostgreSQL (with Hibernate ORM).
  * **Boilerplate Reduction:** Immutable DTOs as Java `record`s; no Lombok — entities expose explicit accessors and beans rely on constructor injection.
  * **API Documentation:** Springdoc OpenAPI 3.0 (Swagger UI).
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
| `ROLE_AGENT` | **IT Specialist (Support Agent)** | Human | Monitors the pool of incoming issues, claims tickets, communicates with users, and marks incidents as resolved. |
| `ROLE_ADMIN` | **Administrator** | Human | Manages reference data (departments, problem categories), registers new employees, and monitors company-wide statistics. |
| `SYSTEM_DAEMON` | **System Scheduler** | Machine | Background cron runner for automated escalation and SLA audits. |

---

## 4. The 7 Atomic Business Processes (State Machine)

The ticket lifecycle is modeled as a deterministic finite-state machine. No composite or hidden transactions are permitted.

### Valid Statuses (`TicketStatus` Enum):
* `NEW` — Created, waiting in the unassigned queue.
* `IN_PROGRESS` — Claimed by an IT specialist, currently being addressed.
* `WAITING_USER` — Paused by the IT specialist pending user response/clarification.
* `RESOLVED` — Marked as fixed by the IT specialist; awaiting employee verification.
* `CLOSED` — Final state; resolution confirmed by the author.

### State Transition & Authority Matrix:

| Process ID | Process Name | Current Status | Target Status | Authorized Actor | System Action & Side Effects |
|:---:|:---|:---|:---|:---|:---|
| **BP-1** | **Ticket Registration** | *None* | `NEW` | `EMPLOYEE` | Creates ticket record; `assignee` is set to `null`; sets `created_at`. |
| **BP-2** | **Claim Ticket** | `NEW` | `IN_PROGRESS` | `AGENT` | Sets `assignee_id = authenticated_agent_id`; moves status to `IN_PROGRESS`. |
| **BP-3** | **Add Comment** | *Any active* | *Unchanged* | `EMPLOYEE`, `AGENT` | Appends a new immutable message to `comments` linked to the ticket. |
| **BP-4** | **Pause Ticket** | `IN_PROGRESS` | `WAITING_USER` | `AGENT` | Toggles status to indicate pending user action. |
| **BP-5** | **Resolve Ticket** | `IN_PROGRESS`, `WAITING_USER` | `RESOLVED` | `AGENT` (Assignee) | Sets status to `RESOLVED`; locks specialist editing actions. |
| **BP-6** | **Confirm & Close** | `RESOLVED` | `CLOSED` | `EMPLOYEE` (Author) | Sets status to `CLOSED`; ticket moves to archive. |
| **BP-7** | **Reopen Ticket** | `RESOLVED` | `IN_PROGRESS` | `EMPLOYEE` (Author) | Reverts status to `IN_PROGRESS`; requires an explanation comment. |

---

## 5. Relational Database Schema (5 Entities)

All tables must be normalized (3NF). Class names must be **singular**, and table names must be **plural**.

```text
 [ departments ] 1 ─── N [ users ] 1 ─── N [ comments ]
                             │                 │
                             │ (creator /      │
                             │  assignee)      │
                             ▼                 ▼
 [ categories ]  1 ──────── N [ tickets ] 1 ───┘
```

### Table Definitions:

1. **`departments`**
    * `id`: `BIGINT` (Primary Key, Identity)
    * `name`: `VARCHAR(100)` (Not Null, Unique)

2. **`users`**
    * `id`: `BIGINT` (Primary Key, Identity)
    * `full_name`: `VARCHAR(150)` (Not Null)
    * `email`: `VARCHAR(100)` (Not Null, Unique)
    * `password`: `VARCHAR(255)` (Not Null, BCrypt hashed)
    * `role`: `VARCHAR(20)` (Not Null, Enum: `EMPLOYEE`, `AGENT`, `ADMIN`)
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
    * `creator_id`: `BIGINT` (Foreign Key -> `users.id`, Not Null)
    * `assignee_id`: `BIGINT` (Foreign Key -> `users.id`, Nullable)
    * `category_id`: `BIGINT` (Foreign Key -> `categories.id`, Not Null)

5. **`comments`**
    * `id`: `BIGINT` (Primary Key, Identity)
    * `text`: `TEXT` (Not Null)
    * `created_at`: `TIMESTAMP` (Not Null)
    * `ticket_id`: `BIGINT` (Foreign Key -> `tickets.id`, Not Null)
    * `author_id`: `BIGINT` (Foreign Key -> `users.id`, Not Null)

---

## 6. Backend Architecture Guidelines

### 6.1. Modular Feature Packaging Blueprint ("One Package - One Entity")
Each domain module must strictly replicate the enterprise package anatomy established in the project.

For any entity `<Entity>` (e.g., `Department`, `User`, `Category`, `Ticket`, `Comment`), create the following internal subpackages:

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
* **JWT:** Tokens are signed with HS256 using a secret of at least 32 bytes (from `app.jwt.secret`, overridable via `JWT_SECRET`). Tokens carry `sub` (email), `uid` and `role` claims with a 24-hour expiry; every request re-validates that the user still exists.
* **Login rate limit:** Failed login attempts per email are limited (`app.rate-limit.max-attempts`, default 5 per 5 minutes) and respond `429 Too Many Requests`.
* **CORS:** The frontend origin(s) are configured via `app.cors.allowed-origins` (default `http://localhost:5173`).

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

All authenticated endpoints expect the JWT token in the `Authorization: Bearer <token>` header. The token is issued with a 24-hour validity and encodes the `role` of the user (used by `hasRole(...)` checks). Public paths always include `/auth/login` and the Swagger UI paths.

**Resource Masking:** All API paths (except `/auth/login` and Swagger paths) are protected. When a request is unauthenticated, carries an invalid token, or lacks the required role, the server responds with `404 Not Found` and a generic `{"message": "Ресурс не найден"}` body — never `401` or `403` — so that the existence of resources is not disclosed. Authorized clients receive normal statuses (`200`, `201`, `204`, `400`, `404` for validation/missing, `409` for conflicts).

### 7.2. Tickets (Core Domain Workflows)
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `POST` | `/tickets` | Register a new ticket (BP-1) | `EMPLOYEE` | `201 Created`, `400 Bad Request` |
| `GET` | `/tickets` | List tickets (with filters by status/role) | Authenticated | `200 OK` |
| `GET` | `/tickets/{id}` | Get full ticket details with comments | Authenticated | `200 OK`, `404 Not Found` |
| `PATCH`| `/tickets/{id}/assign` | Claim ticket into work (BP-2) | `AGENT` | `200 OK`, `409 Conflict` |
| `PATCH`| `/tickets/{id}/status` | Transition status (BP-4, 5, 6, 7) | `EMPLOYEE`, `AGENT` | `200 OK`, `400 Bad Request` |

### 7.3. Comments
| HTTP Method | Endpoint | Description | Permitted Roles | Expected Status |
|:---|:---|:---|:---|:---|
| `POST` | `/tickets/{id}/comments`| Post a message in the ticket thread (BP-3)| Authenticated | `201 Created`, `400 Bad Request` |
| `GET` | `/tickets/{id}/comments` | List messages of the ticket thread in chronological order | Authenticated | `200 OK`, `404 Not Found` |

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
| `GET` | `/users/{id}` | Get user profile by ID | `ADMIN`, Self | `200 OK`, `404 Not Found` |
| `POST` | `/users` | Provision a new user account | `ADMIN` | `201 Created`, `409 Conflict` |
| `PATCH`| `/users/{id}` | Partially update user profile / department / role | `ADMIN` | `200 OK`, `404 Not Found` |
| `DELETE`| `/users/{id}` | Deactivate or remove user | `ADMIN` | `204 No Content`, `404 Not Found` |

---

## 8. Frontend Architecture & React Guidelines

The frontend is a modern React SPA located in `/frontend`, consuming the Spring Boot REST API.

### 8.1. Directory Structure
```text
frontend/
├── src/
│   ├── api/                      <-- Axios client and modular API functions
│   │   ├── client.ts             (baseURL: '/api/v1', interceptors)
│   │   ├── authApi.ts
│   │   └── ticketsApi.ts
│   ├── types/                    <-- TypeScript interfaces strictly mirroring Java DTOs
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   └── ticket.ts
│   ├── components/
│   │   ├── Navbar.tsx            (Top header with User info, Role badge, and Logout)
│   │   ├── StatusBadge.tsx       (Color-coded status pills: NEW, IN_PROGRESS, etc.)
│   │   └── CommentThread.tsx     (Discussion timeline + message submission form)
│   ├── pages/
│   │   ├── LoginPage.tsx         (Screen 1: Authentication form)
│   │   ├── TicketListPage.tsx    (Screen 2: Table with filter tabs: All, New, In Progress, Resolved)
│   │   ├── CreateTicketPage.tsx  (Screen 3: Ticket submission form)
│   │   └── TicketDetailPage.tsx  (Screen 4: Adaptive detail view for 4a/4b)
│   ├── App.tsx                   (Router and Role Context)
│   └── main.tsx
├── package.json
└── vite.config.ts
```

### 8.2. UI Screens & Adaptive Action Panel Rules
1. **Screen 1 (Login):** Email and password input, stores authenticated token & user profile (`role`, `fullName`) in client state.
2. **Screen 2 (Ticket List):** Table with columns: Number, Title, Category, Assignee, Status (color-coded badge), Created Date. Filter buttons: `[Все]`, `[Новые]`, `[В работе]`, `[Решённые]`. Displays `[+ Создать заявку]` button only for `ROLE_EMPLOYEE`.
3. **Screen 3 (Create Ticket Form):** Category dropdown, Title input, Description textarea. Validates required fields before submitting to `POST /api/v1/tickets`.
4. **Screen 4 (Ticket Details - Adaptive 4a / 4b View):**
    * Left section: Ticket metadata, full description, and chronological `CommentThread`.
    * Action panel adapts dynamically based on authenticated role and current ticket status:
        * **Agent View (4a):** Shows `[Взять в работу]` if `status === 'NEW'`. Shows `[Жду ответа]` and `[Решено]` if `status === 'IN_PROGRESS'`. Action buttons are hidden if `status === 'RESOLVED'`.
        * **Author View (4b):** Action buttons are hidden while `NEW` or `IN_PROGRESS`. Shows `[Закрыть заявку]` and `[Вернуть на доработку]` ONLY when `status === 'RESOLVED'`.

### 8.3. Type Safety (DTO Mirroring)
Every TypeScript type in `src/types/` must mirror its corresponding Java DTO Record. Example:
```typescript
export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_USER' | 'RESOLVED' | 'CLOSED';

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  departmentName: string;
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
}
```

---

## 9. Build & Verification Commands
* **Start local PostgreSQL:** `docker compose up -d`
* **Run backend application:** `./gradlew bootRun` (serves REST API and Swagger UI at `http://localhost:8080/swagger-ui.html`)
* **Automated seeding:** On first startup `core.config.DBInitializer` idempotently creates the `IT Service Desk` department, four default incident categories and the admin account `admin@company.ru` / `Admin2024!` (password overridable via `ADMIN_PASSWORD`, full name via `ADMIN_FULL_NAME`).
* **Run test suite:** `./gradlew test`
* **Build backend archive:** `./gradlew build -x test`
* **Run frontend dev server:** `cd frontend && npm run dev`
* **Build frontend for production:** `cd frontend && npm run build`