---
name: java21-springboot4-helpdesk
description: Rules for working on the IT Helpdesk backend built with Java 21, Spring Boot 4.1 (Spring Framework 7, Spring Security 7, Hibernate 7, Jackson 3), Gradle 9, MapStruct and springdoc. Use this skill whenever you need to write, change, review or debug Java code, build.gradle, application.properties, Dockerfile, tests or configuration in this repository; and whenever the task involves Spring, JPA/Hibernate, Spring Security/JWT, Jackson, validation, Swagger/OpenAPI, MapStruct or Gradle — even if the user does not mention versions explicitly. By default the model knows Spring Boot 3 and Jackson 2, while this project uses Boot 4 and Jackson 3, so without this skill it is easy to generate code that does not compile or violates project conventions.
---

# Java 21 + Spring Boot 4: IT Helpdesk

This skill describes how to write code in this project so that it compiles on the current stack and matches the style of the existing codebase. It complements `AGENTS.md` (business processes, DB schema, REST specification) and does not replace it.

## 0. Workflow

1. Before any change, read `AGENTS.md` (domain, ticket state machine, role permissions) and find 1–2 similar existing classes. New code must look like its neighbours.
2. Check versions in `build.gradle` instead of recalling them from memory. Your knowledge of Spring is mostly about Boot 3.x; in Boot 4 part of the API has moved or been removed (section 2).
3. If you are unsure about a Boot 4 / Spring Framework 7 API, consult the official Migration Guide and release notes. Do not invent classes or packages.
4. After making changes, run `./gradlew test` (and `./gradlew build -x test` if you touched the build), and follow section 10 of `AGENTS.md` (branches, commits, verification, merge).

## 1. Stack and versions

| Component | Version / decision |
|---|---|
| JDK | Java 21 (LTS), toolchain in `build.gradle` |
| Spring Boot | 4.1.1 (Spring Framework 7, Spring Security 7, Spring Data JPA 4) |
| Jakarta EE | 11 (`jakarta.*` packages, never `javax.*`) |
| ORM | Hibernate ORM 7.x, Jakarta Persistence 3.2, PostgreSQL |
| JSON | Jackson 3 (`tools.jackson.*`) |
| Build | Gradle 9.6 (Groovy DSL), `platform(spring-boot-dependencies)` + the `org.springframework.boot` plugin |
| Mapping | MapStruct 1.6.3, `-Amapstruct.defaultComponentModel=spring` |
| API docs | springdoc-openapi 3.1.1 (`springdoc-openapi-starter-webmvc-ui`), Swagger UI |
| Security | Spring Security + `spring-security-oauth2-jose` (Nimbus) for HS256 JWT, BCrypt |
| Tests | JUnit 5 + Mockito (`MockitoExtension`), without starting a Spring context |
| No Lombok | entities with explicit getters/setters, DTOs are `record`s |

Root package: `ru.basted.ithelpdesk`. API base path: `/api/v1`.

## 2. Boot 3 → Boot 4: how it differs from what you remember

This is the most common source of errors. Check yourself against the table.

| Boot 3 habit | Correct approach in Boot 4 |
|---|---|
| `com.fasterxml.jackson.databind.*` (`ObjectMapper`, `JsonNode`, …) | `tools.jackson.databind.*`. Annotations stayed in `com.fasterxml.jackson.annotation.*` |
| `JsonProcessingException` (checked) | `tools.jackson.core.JacksonException` — unchecked |
| `@JsonComponent`, `Jackson2ObjectMapperBuilderCustomizer` | `@JacksonComponent`, `JsonMapperBuilderCustomizer` |
| `spring.jackson.read.*` / `spring.jackson.write.*` | `spring.jackson.json.read.*` / `spring.jackson.json.write.*` |
| `@MockBean`, `@SpyBean` | Removed. Use only `@MockitoBean`, `@MockitoSpyBean` |
| `@WebMvcTest`, `@DataJpaTest` from `org.springframework.boot.test.autoconfigure.*` | Moved into modules, e.g. `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`; test starters are required (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`) |
| `spring-boot-starter-web` | The canonical name is `spring-boot-starter-webmvc`. The old name is a deprecated alias; it still works in this project, so do not rename it unless asked, but use the new name for new dependencies |
| Flyway/Liquibase "come for free" | You need `spring-boot-starter-flyway` / `spring-boot-starter-liquibase` |
| `org.springframework.lang.Nullable` / `@NonNull` | JSpecify: `org.jspecify.annotations.Nullable` / `NonNull` / `@NullMarked`. The project already does this (`JwtAuthenticationFilter`, `RestAuthenticationHandler`) |
| Spring Retry (`spring-retry`) | Built into Framework 7: `@Retryable`, `@ConcurrencyLimit`, `@EnableResilientMethods` (`org.springframework.core.retry`). Do not add the spring-retry dependency |
| `RestTemplate` for new clients | `RestClient`; declarative HTTP clients via `@ImportHttpServices` |
| `TestRestTemplate`/`WebTestClient` for MVC tests | `RestTestClient` is available for MVC (new in Framework 7) |
| Undertow | Not supported; Tomcat/Jetty only |

If there is a working example in the repository for a doubtful spot, copy its approach rather than relying on memory.

## 3. Java 21

Use (already applied in the project):
- `record` for DTOs and internal values (`private record AttemptWindow(...)`).
- `switch` expressions over enums and pattern matching in `instanceof` (`if (!(uidClaim instanceof Number uid)) …`).
- Pattern matching in `switch` and record patterns (final in Java 21) — when they genuinely simplify the code; for `sealed` hierarchies an exhaustive `switch` without `default` gives compiler verification.
- Text blocks for long examples (`@ExampleObject`) and JPQL.
- `Stream.toList()` (immutable list), `String.formatted()`, `var` for obvious local types.
- Sequenced collections: `getFirst()`, `getLast()`, `reversed()` instead of `list.get(0)` / manual reversal.

Do not use:
- Java 21 preview features (string templates, unnamed patterns/variables `_`, unnamed classes, scoped values). The project does not enable `--enable-preview`.
- Lombok or any getter code generation — this is a deliberate prohibition in the project.

Virtual threads (`spring.threads.virtual.enabled=true`) are not enabled in the project. Do not enable them in passing: on JDK 21, `synchronized` blocks and native calls can "pin" the carrier thread, so this is a separate decision that requires measurements.

## 4. Layers and packages ("one package — one entity")

```text
ru.basted.ithelpdesk.<feature>/
├── controller/  <Entity>API.java (interface: OpenAPI + mappings + @PreAuthorize + @Valid)
│                <Entity>Controller.java (@RestController, delegation only)
├── dto/         records: CreateRequest / ChangeName / UpdateRequest / Response
├── entity/      JPA entity and enums
├── mapper/      MapStruct interface
├── repository/  Spring Data JpaRepository
└── service/     <Entity>Service (interface) + <Entity>ServiceImpl (@Service, @Transactional)
```

When creating a new entity, create all subpackages following this template. Shared things go into `core/` (`config`, `exception`, `model/BaseEntity`, `security/CurrentUser`).

Dependencies go top-down: controller → service → repository. A controller knows nothing about repositories and entities. An entity is never returned to the outside.

### Controller and API interface

The contract lives in the interface; the implementation is "clean":

```java
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController implements TicketAPI {
    private final TicketService service;

    public TicketController(TicketService service) { this.service = service; }

    @Override
    public ResponseEntity<TicketResponse> createTicket(TicketCreateRequest createRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTicket(createRequest));
    }
}
```

In the `<Entity>API` interface place `@Tag`, `@Operation`, `@ApiResponses`, `@GetMapping`/`@PostMapping`…, `@PreAuthorize`, `@Valid @RequestBody`, and `@Min`/`@Parameter` on parameters. The implementation has none of these annotations — Spring MVC and method security collect metadata through the type hierarchy, so there is no need to duplicate them.

Do not put `@Validated` on controllers: parameter validation is handled by the built-in Spring MVC mechanism (`HandlerMethodValidationException`), and `ApiExceptionHandler` already has a handler for it. `@Validated` would switch the behaviour to AOP and change the exception type.

### Service

- Interface + `@Service` implementation, dependencies injected **through the constructor only**, fields are `final`. No `@Autowired` on fields.
- `@Transactional` on mutating methods of the implementation, `@Transactional(readOnly = true)` on reading ones.
- Ownership checks and business rules belong in the service (not in the controller). The user is obtained via `CurrentUser.get()`.
- Violation of state-machine rules → `ConflictException` (409); missing entity → `NotFoundException` (404). Project pattern: small helpers `checkRule(boolean, String)` and `checkOwnership(...)`.
- Each action = one transparent state change (the "atomic" process principle from `AGENTS.md`). Do not merge several business processes into one method.

## 5. DTOs, validation, localization

- All DTOs are `record`s. Request DTOs accept identifiers (`Long categoryId`); Response DTOs return human-readable fields (`String categoryName`) or nested records (`UserSummary`).
- We do not use `@JsonView` — create separate Request/Response types.
- Jakarta Validation constraints (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, …) go on record components, each with a meaningful Russian `message`. A controller must have `@Valid`.
- `@Schema(description = …, example = …)` goes on every DTO component, with texts in Russian.
- E-mail addresses are normalized (`trim` + lower-case) in the service before persistence; uniqueness is enforced by a `lower(email)` index and login stays case-insensitive.

```java
@Schema(description = "Модель данных для создания новой заявки")
public record TicketCreateRequest(
        @NotBlank(message = "Заголовок заявки не может быть пустым")
        @Size(max = 200, message = "Заголовок заявки не может быть длиннее 200 символов")
        @Schema(description = "Заголовок заявки", example = "Не работает корпоративная почта")
        String title,
    ...
) {}
```

**Language:** class, method, table and URL path names are in English. Everything that a user or an API developer sees (Swagger, validation messages, error texts, exception messages) is **strictly in Russian**. This rule is intentional and stays in force even though this document is written in English.

## 6. Errors

The single point is `core.exception.ApiExceptionHandler` (`@RestControllerAdvice`); the error body is `ApiError(message)`.

| Situation | Exception | HTTP |
|---|---|---|
| Entity not found | `NotFoundException` | 404 |
| Duplicate / invalid transition | `ConflictException` | 409 |
| DB integrity violation | `DataIntegrityViolationException` | 409 |
| Stale optimistic-lock update | `OptimisticLockingFailureException` | 409 |
| Validation, unreadable body, wrong parameter type | handled in the handler | 400 |
| Login attempt limit exceeded | `RateLimitExceededException` | 429 |

**Resource masking (important):** an unauthenticated request, an invalid token and insufficient permissions always produce `404` with the body `{"message":"Ресурс не найден"}` ("Resource not found") — never `401`/`403`. This is done by `RestAuthenticationHandler` (both `AuthenticationEntryPoint` and `AccessDeniedHandler`). Therefore:
- do not add `@ExceptionHandler(AccessDeniedException.class)` to `ApiExceptionHandler` — it would intercept the exception earlier and break the masking;
- do not return 401/403 from controllers or services;
- in Swagger, document 404 with the example "Ресурс не найден" for protected methods.

## 7. Security (Spring Security 7)

- Lambda DSL only: `http.csrf(...).cors(...).sessionManagement(...).authorizeHttpRequests(...)`. The `and()` and `authorizeRequests()` methods are removed. `MvcRequestMatcher`/`AntPathRequestMatcher` are gone too — use `requestMatchers("/path/**")` (strings) or `PathPatternRequestMatcher`.
- The application is stateless: `SessionCreationPolicy.STATELESS`, CSRF disabled, CORS configured through `app.cors.allowed-origins`.
- Method-level authorization: `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` in `<Entity>API`. For "the user themself", the `UserSecurity.isSelf(id)` bean is used.
- JWT: HS256, secret ≥ 32 bytes (`app.jwt.secret` / `JWT_SECRET`), claims `sub` (email), `uid`, `role`, lifetime 24 h, issuer `ithelpdesk`. The implementation is `JwtService` on `NimbusJwtEncoder/Decoder`. On every request `JwtAuthenticationFilter` re-reads the user from the DB and takes the role **from the DB**, not from the token — a role change takes effect immediately. Preserve this property.
- Passwords — only through `PasswordEncoder` (BCrypt); a password never appears in any Response DTO or in logs.
- Roles: `EMPLOYEE`, `AGENT`, `MANAGER`, `ADMIN` (+ the machine role `SYSTEM_DAEMON` for the scheduler). See the permission matrix in `AGENTS.md`; a manager cannot take tickets, change statuses or write messages.
- For security tests with `@WithMockUser`, Boot 4 requires `spring-boot-starter-security-test`.

## 8. JPA / Hibernate 7

- An entity extends `BaseEntity` (`@MappedSuperclass`, `@GeneratedValue(strategy = IDENTITY)`, `Long id`). The class name is singular, the table name is plural (`@Table(name = "tickets")`).
- All `@ManyToOne` use `fetch = FetchType.LAZY`; foreign keys are named explicitly: `foreignKey = @ForeignKey(name = "fk_<table>_<column>")`.
- Enums use `@Enumerated(EnumType.STRING)` with a `length`. Time is `LocalDateTime`, columns like `created_at`.
- `spring.jpa.open-in-view=false`. Consequence: lazy associations cannot be read outside a transaction. Either map to a DTO inside `@Transactional`, or load associations up front with `@EntityGraph(attributePaths = {...})` on repository methods (as in `TicketRepository`). This protects against `LazyInitializationException` and N+1.
- Queries are derived query methods; for non-trivial ones use `@Query` with a text block. Do not pass an `Entity` from a repository to a controller.
- Statistics are aggregated in the database (`GROUP BY`/`COUNT`/projections), never by loading whole tables into memory.
- Schema changes are made only through Flyway migrations in `src/main/resources/db/migration` (`spring-boot-starter-flyway` + `flyway-database-postgresql`); `spring.jpa.hibernate.ddl-auto=validate`. The baseline `V1` owns tables, constraints, indexes and reference data. `DBInitializer` only creates the admin account. Never add DDL, backfills or data repair to application code.
- `Ticket` and `TicketRequest` use `@Version` optimistic locking. Taking a ticket uses an atomic `UPDATE ... WHERE status = 'NEW'` that also checks the agent capacity. `ApiExceptionHandler` maps optimistic-lock failures to `409` with a Russian message.

## 9. MapStruct

- `@Mapper` interfaces without `componentModel` — it is set globally in `build.gradle` (`-Amapstruct.defaultComponentModel=spring`), so a `@Component` is generated.
- Mappers build an entity from a Request DTO. Fields set by the service (status, dates, associations, `creator`) are ignored explicitly: `@Mapping(target = "status", ignore = true)`. This way the compiler does not let you silently forget a field.
- Response DTOs with nested data are often assembled manually in the service in this project (`toResponse`) — that is fine when business logic is needed (SLA, `overdue`, `dueAt`).
- The processor is connected as `annotationProcessor`; if other processors are added (should they ever be needed), watch the order.

## 10. Jackson 3

- Import `tools.jackson.databind.ObjectMapper` (as in `RestAuthenticationHandler`). Spring Boot provides a `JsonMapper` bean, which is also an `ObjectMapper`; inject it through the constructor.
- Do not bring in Jackson 2 and do not add `com.fasterxml.jackson.databind` to imports — it will not compile.
- `LocalDateTime` is serialized as an ISO string (`2026-10-18T10:15:30`); the frontend and Swagger examples rely on this format.
- Settings go through `spring.jackson.*` (taking into account the move of `read/write` to `json.read/json.write`) or `JsonMapperBuilderCustomizer`.

## 11. Configuration

- Parameters go in `application.properties` as `${ENV_VAR:default}`; in code use `@Value("${app.xxx:default}")` in the constructor. Add a new property with the `app.` prefix together with an environment variable, and if needed to `.env.example` and `docker-compose*.yml`, and mention it in `AGENTS.md`.
- Do not hardcode or log secrets. The default `JWT_SECRET` and the seeded admin password are acceptable only for local development.
- Scheduler: `@EnableScheduling` on `Application`, tasks are `@Scheduled(fixedDelayString = "${…}")` + `@Transactional` in a service (`TicketAutoCloseService`). Move intervals into properties.
- Login rate limiting: `LoginRateLimiter` removes expired windows through `@Scheduled` (`app.rate-limit.cleanup-ms`, env `RATE_LIMIT_CLEANUP_MS`). Keep the map bounded when adding new keys.
- Flyway: migrations run at startup. `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1` let an already upgraded production database be baselined without re-running `V1`. Never edit an applied migration; add a new one.
- Swagger: `/swagger-ui.html` and `/v3/api-docs` are public paths; common OpenAPI settings live in `OpenApiConfig` (the `BearerAuth` scheme).

## 12. Tests

- Style: pure unit tests of services — `@ExtendWith(MockitoExtension.class)`, `@Mock` for dependencies, the service is created with `new` in `@BeforeEach`. The Spring context is not started, so tests are fast.
- JUnit 5 (`org.junit.jupiter.api.*`), `assertEquals`/`assertThrows`, `verify(..., never())`.
- Method names are in English and describe behaviour (`reopenWithoutReasonThrowsConflict`); test data is in Russian, like the messages.
- For every new state-machine rule, write a test for success and for violation (`ConflictException`), plus a check that `save` is not called on refusal.
- If slice or integration tests are needed: `@MockitoBean` instead of `@MockBean`, the current `@WebMvcTest`/`@DataJpaTest` packages and test starters (section 2). First check that the dependency is added to `build.gradle`.
- Gradle 9 requires an explicit `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` for JUnit — it is already there, do not remove it.

## 13. Build and deployment

- Gradle Wrapper 9.6, run only through `./gradlew`. Java 21 toolchain; do not raise the language version without a request.
- Commands: `./gradlew test`, `./gradlew build -x test`, `./gradlew bootRun`, `docker compose up -d` (PostgreSQL).
- The `Dockerfile` is multi-stage (`eclipse-temurin:21-jdk` → `21-jre-alpine`), `bootJar`, runs as a non-root user, healthcheck on `/v3/api-docs`. Preserve this when editing.
- Keep library versions in the `ext {}` block in `build.gradle`; the Spring Boot BOM is attached via `platform(...)`. Do not add versions to dependencies managed by the BOM.

## 14. What not to do

- Do not return `@Entity` from controllers, do not use `@JsonView`.
- Do not add Lombok, field injection, or `@Autowired` on fields.
- Do not write 401/403 — only masking to 404.
- Do not put Swagger annotations in controllers — only in `<Entity>API`.
- Do not write error messages or `@Schema` in English.
- Do not import `com.fasterxml.jackson.databind`, `javax.*`, `org.springframework.lang.*`.
- Do not use `@MockBean`/`@SpyBean`, `and()` in the Security DSL, or `authorizeRequests`.
- Do not enable Java preview features and do not switch to `RestTemplate` in new code.
- Do not change the semantics of statuses and permissions without checking the BP-1…BP-13 process table in `AGENTS.md`.
- Do not leave dead code behind: unused imports, private methods, repository methods, DTO fields, properties and dependencies must be removed together with the change that made them unused.
- Do not load whole tables to filter, count or group in memory — use repository queries; do not call `save()` on an already managed entity inside a transaction without need.
- Do not add application-side backfills, data repair or fallbacks for legacy data. Schema and reference data change only through Flyway migrations; one-off production normalization is performed by the database administrator with a script.
- Do not copy date formatting, route guards or role checks between files when a shared helper already exists.

## 15. Known weak spots (do not replicate; suggest a fix when appropriate)

- `IllegalArgumentException` as a whole turns into 400 — a dedicated domain exception would be better.
- `JwtAuthenticationFilter` is a `@Component` filter, which Boot also registers in the servlet container.

When you work next to one of these spots, mention it to the user instead of silently fixing it as part of an unrelated task. When a weak spot is fixed, remove it from this list in the same change.

## 16. Checklist before handing over

- [ ] New code repeats the structure of a neighbouring module (packages, names, style).
- [ ] No Lombok, field injection, `javax`, Jackson 2 or `org.springframework.lang` imports.
- [ ] DTOs are records with Russian `message`s and `@Schema`; the controller uses `@Valid`.
- [ ] OpenAPI and `@PreAuthorize` are in `<Entity>API`; public paths are not widened without need.
- [ ] Mutating service methods are `@Transactional`; lazy associations are not read outside a transaction.
- [ ] Errors are `NotFoundException`/`ConflictException` with Russian messages.
- [ ] No dead code left (unused imports, methods, repository queries, properties, dependencies); no in-memory filtering or aggregation of whole tables.
- [ ] Unit tests added/updated; `./gradlew test` passes.
- [ ] If the schema, API or processes changed — `AGENTS.md` and (if needed) frontend types are updated.