# RunCoach

Scaffolding for an AI-powered running coach. Java 21 / Quarkus 3 modular monolith,
PostgreSQL / Flyway, and a feature-first Flutter / Material 3 client.

This is not a finished product. No training, recovery, readiness, sleep analysis,
nutrition, hydration, race-prediction, or AI decision logic is implemented.
The Athlete module implements authenticated self-profile creation, reading and
updates. Training adds interface-first planning contracts and deliberately
unimplemented generation endpoints. Subscription and payment modules implement
trial/lifecycle/entitlement and verified-payment orchestration, with checkout
disabled until configured. Other modules remain scaffolds. UI pages display empty states.

## Repository

```text
.
|-- backend/
|   |-- pom.xml
|   `-- src/
|       |-- main/
|       |   |-- java/io/runcoach/
|       |   |   |-- auth/
|       |   |   |-- athlete/
|       |   |   |-- goal/
|       |   |   |-- health/
|       |   |   |-- readiness/
|       |   |   |-- training/
|       |   |   |-- nutrition/
|       |   |   |-- analytics/
|       |   |   |-- ai/
|       |   |   |-- cache/
|       |   |   |-- subscription/
|       |   |   |-- payment/
|       |   |   |-- common/
|       |   |   |-- config/
|       |   |   `-- exception/
|       |   `-- resources/
|       |       |-- application.properties
|       |       `-- db/migration/V{1,2,3,4}__*.sql
|       `-- test/java/io/runcoach/{athlete,cache,ai,training,billing,payment,subscription}/
|-- frontend/
|   |-- pubspec.yaml
|   |-- lib/
|   |   |-- main.dart
|   |   |-- app.dart
|   |   |-- core/{config,network}/
|   |   |-- routes/
|   |   |-- theme/
|   |   |-- shared/{widgets,services,models,utils}/
|   |   `-- features/
|   |       |-- onboarding/
|   |       |-- authentication/
|   |       |-- dashboard/
|   |       |-- training/
|   |       |-- nutrition/
|   |       |-- recovery/
|   |       |-- analytics/
|   |       |-- ai_coach/
|   |       |-- profile/{domain,data,presentation}/
|   |       `-- subscription/
|   |-- test/
|   `-- web/
|-- deployment/
|   |-- docker/{Dockerfile,Dockerfile.flutter,docker-compose.yml}
|   |-- kubernetes/
|   |-- nginx/nginx.conf
|   `-- helm/runcoach/
|-- scripts/
|   |-- run-local.sh
|   |-- build-all.sh
|   |-- clean-all.sh
|   |-- migrate-db.sh
|   |-- create-module.sh
|   `-- generate-openapi.sh
`-- .github/workflows/ci.yml
```

All thirteen backend modules contain `entity`, `dto`, `repository`, `service`,
`resource`, `mapper`, and `validator` packages. Unused packages contain a
`package-info.java` placeholder rather than dummy business classes.

## Architecture

One deployable Maven application, one PostgreSQL database, explicit domain
packages. This is a package-based modular monolith, not eleven independently
deployed services. Its structure is Clean Architecture-inspired: domain-facing
contracts sit beside application services, with Panache, REST and CDI adapters.
JPA entities intentionally use Jakarta annotations; strict framework-free domain
objects can be introduced when actual domain rules are specified.

Request flow: resource -> service -> repository. Resources handle transport;
services orchestrate use cases; repositories handle persistence; mappers separate
entities from wire DTOs; validators own input checks. No JPA entity is returned
by the sample API.

| Module | Intended responsibility |
| --- | --- |
| auth | JWT identity boundary and future account provisioning |
| athlete | Athlete profile and account-lock application contracts |
| goal | Athlete targets |
| health | Imported sleep, heart metrics and recovery inputs/results |
| readiness | Deterministic readiness engine boundary |
| training | Deterministic plans, workouts and recorded activities |
| nutrition | Deterministic nutrition and hydration engine boundary |
| analytics | Reporting and future deterministic race prediction |
| ai | Explain previously calculated recommendations only |
| cache | Optional best-effort caching |
| subscription | Trial, plan catalog, lifecycle and feature entitlements |
| payment | Checkout, verification, webhook processing and invoices |
| common | Small shared persistence/transport primitives, not shared business rules |

The new billing modules own the V4 tables. Original V3 `subscriptions` and
`payments` placeholders remain untouched. V4 refuses to migrate populated legacy
billing tables without an explicit data reconciliation step. Billing details and
configuration are documented in [docs/BILLING.md](docs/BILLING.md).

Access another module through its application contract or DTO, never its
repository/entity. Keep cross-module references as UUIDs, not JPA object graphs.
Foreign keys currently protect monolith consistency, including composite
ownership checks on goals/plans/workouts/payments. Microservice extraction must
replace these cross-boundary constraints deliberately; it is not automatic.
Package boundaries are a convention in this scaffold, not yet enforced by
ArchUnit/JPMS. No event bus or distributed transactions are introduced.

## Prerequisites

- JDK 21, Maven 3.9+; ensure `JAVA_HOME` points to JDK 21.
- Flutter 3.35.7 recommended (Dart 3.9); minimum Flutter 3.32 / Dart 3.8.
- Docker with Compose v2 for PostgreSQL and optional Redis.
- Bash on Linux/macOS, WSL or Git Bash on Windows for helper scripts.
- Kubernetes/Helm only for the corresponding deployment templates.

## Local Backend

Run from the repository root in Bash:

```bash
cp .env.example .env
# Edit .env: keep POSTGRES_USER/DB_USERNAME and POSTGRES_PASSWORD/DB_PASSWORD aligned.
# Set JWT_ISSUER and JWT_PUBLIC_KEY_LOCATION to your identity provider.
bash scripts/run-local.sh
```

The script loads the trusted local `.env` as Bash environment variables, starts
PostgreSQL, and runs Quarkus dev mode on `http://localhost:8080`. Use shell-safe
dotenv values and do not source an untrusted `.env`. Never commit credentials.
Flyway migrates at startup; Hibernate validates mappings rather than altering
the schema. PostgreSQL/Redis Dev Services are disabled for reproducible startup.

For container-only startup:

```bash
docker compose --env-file .env -f deployment/docker/docker-compose.yml up --build -d
```

For Redis, set `CACHE_ENABLED=true` in `.env`, then include `--profile cache` in
the Compose command. The dev backend uses `REDIS_URL` from `.env`; a container
backend uses the internal Redis hostname. PostgreSQL is required; Redis is not.

## Authentication And Athlete API

JWT verification uses an external issuer and public key/JWKS URL. No signing
keys, token issuance, passwords, identity provider, login flow, account creation
with the identity provider, or production mock bypass is included. Only health endpoints are public. The configured
issuer/JWKS placeholders must be replaced before authenticated requests work.

```bash
curl http://localhost:8080/q/health/ready
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/v1/athletes/me
```

`GET /api/v1/athletes/me` looks up `users.auth_subject` using verified JWT `sub`.
It returns `id`, `displayName`, `timeZone`, or 404 when no profile is provisioned.
A valid token alone does not provision a user: create the profile explicitly
with POST. All Athlete operations derive ownership from JWT `sub`, never a
caller-selected user ID. Use one configured identity issuer; multi-issuer
subject namespacing is not implemented.

| Method | Path | Behavior |
| --- | --- | --- |
| POST | /api/v1/athletes/me | Create the current identity's profile; returns 201, Location and ETag |
| GET | /api/v1/athletes/me | Read the current profile; returns 200 and ETag |
| PUT | /api/v1/athletes/me | Replace displayName and timeZone with a required If-Match precondition |

Create requires `email`, `displayName` and `timeZone`. Email is a self-reported
contact address, not a verified identity claim or authorization input. Neither
the subject nor email is editable through PUT. Display names are trimmed at
mapping time. Named time zones must exist in the server's Java TZDB, including
`UTC` and names such as `Asia/Bangkok`; bare numeric offsets are rejected.
Jakarta Validation enforces required values, email shape and database-aligned
length limits on DTOs/entities and service request parameters.

```bash
curl -i -X POST http://localhost:8080/api/v1/athletes/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"email":"runner@example.com","displayName":"Alex Runner","timeZone":"Asia/Bangkok"}'

curl -i http://localhost:8080/api/v1/athletes/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# Use the ETag returned by the latest GET/POST/PUT, including its quotes.
curl -i -X PUT http://localhost:8080/api/v1/athletes/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -H 'If-Match: "0"' \
  -d '{"displayName":"Alex Runner","timeZone":"UTC"}'
```

The JSON response shape remains `id`, `displayName`, `timeZone`; the JPA version
is carried only in a strong ETag. Profile responses are private/non-cacheable,
and browser CORS exposes `ETag`/`Location`. GET does not implement conditional
304 responses. PUT accepts exactly one quoted non-negative version, not a tag
list or wildcard. Missing If-Match returns 428; malformed tags/invalid fields
return 400; weak or stale tags return 412. On 412, retrieve the latest profile
and reconcile changes before resubmitting. An unchanged PUT may retain its ETag.

Duplicate creation returns 409. The pre-check is backed by the existing
`users_auth_subject_key` unique constraint to catch concurrent creates. Updates
use the inherited JPA `@Version` and flush inside the transaction so an
optimistic-lock race also returns 412. There are deliberately no administrative
list, arbitrary-ID access, profile deletion or email-change endpoints.

The REST exception mapper returns a sanitized `ApiError`. Authentication
failures that occur before REST dispatch may use Quarkus' native error format;
Jakarta request validation uses Quarkus' native constraint-violation response.
Uniform gateway/auth/validation error envelopes are future work.

OpenAPI schemas are generated at build time:

```bash
bash scripts/generate-openapi.sh
# backend/target/openapi/openapi.yaml and openapi.json
```

Runtime `/q/openapi` and dev `/q/swagger-ui` require authentication. Do not expose
management/documentation routes publicly in production. Nginx blocks `/q/`.

## Database

| Migration | Tables |
| --- | --- |
| V1 | users, goals, training_plans, workouts, activities |
| V2 | sleep_records, heart_metrics, recovery_scores, readiness_scores, nutrition_plans |
| V3 | subscriptions, payments, audit_logs |
| V4 | subscription_plan, user_subscription, subscription_feature, payment_transaction, payment_method, payment_webhook_log, billing_invoice |

UUID identifiers; optimistic locking on mutable aggregates; UTC-aware
timestamps; date-specific athlete values use a date column plus profile time
zone; explicit units; integer minor currency units; no payment card storage.
Append-only audit records deliberately have no mutable-entity columns.

These are proposed starter schemas, not validated product/business contracts.
The 0-100 score scale, daily score/plan uniqueness, date handling, workout types,
provider statuses, retention, consent and deletion policy must be agreed before
business implementation. No scoring thresholds or calculation formulas exist.
Cross-module foreign keys do not imply permission to query another repository.

```bash
bash scripts/migrate-db.sh
```

This runs Flyway in a one-shot container against local Compose PostgreSQL.
Production should use a dedicated migration job with a privileged migration
role; use a restricted runtime database role. Startup migration is currently
enabled for convenient local scaffolding. Never edit an applied migration.
Timestamp updates performed outside JPA must maintain `updated_at` explicitly.

## Training Module

Training is contract-first. No training algorithm, workout prescription,
adaptive adjustment, or plan persistence use case is implemented.

| Component | Contract |
| --- | --- |
| TrainingService | Application interface for full-plan, weekly and single-workout generation |
| DefaultTrainingService | Validates date windows, resolves the current athlete through AthleteService and constructs planning context |
| TrainingPlanner | Separate plan, week, workout and future adaptive planning methods |
| TrainingPlanEngine | Existing engine port now extends TrainingPlanner |
| RuleEngine | Evaluates a candidate against server-side context and returns structured rule violations; no implementation or automatic approval |
| UnimplementedTrainingPlanner | Default CDI adapter; every operation throws TrainingPlanningUnavailableException |
| TrainingResource | Authenticated REST transport and OpenAPI declarations |

`TrainingPlanEntity` and `WorkoutEntity` map the existing `training_plans` and
`workouts` tables, inheriting UUIDs, audit timestamps and optimistic-lock versions.
Owner/plan references are scalar UUIDs rather than cross-module object graphs.
Jakarta Validation mirrors mandatory fields, date order, numeric precision and
non-negative metric constraints. The persisted plan's optional goal mapping is
preserved; generation requests require a goal ID. No migration changes are needed.

The service obtains athlete ID/time zone from the authenticated profile, not
request fields. `TrainingPlanningContext` currently carries athlete/goal
references and a date window, **not** a resolved goal target or health snapshot.
Before enabling an algorithm, implement goal existence/ownership resolution
through the goal module's application contract and provide approved typed goal
and athlete inputs. Never read another module's repositories directly.

`PlannedTrainingPlan`, `PlannedWeek` and `PlannedWorkout` describe future drafts,
not saved entities. Plan/week workout collections are defensively copied. The
weekly window is seven inclusive dates starting at `weekStart`; no Monday rule,
weekly mileage, intensity distribution or progression policy is invented.

| Method | Path | Request fields |
| --- | --- | --- |
| POST | /api/v1/training/plans/generate | goalId, startDate, endDate |
| POST | /api/v1/training/weeks/generate | goalId, weekStart |
| POST | /api/v1/training/workouts/generate | goalId, scheduledDate |

For authenticated callers with a provisioned athlete and valid input, all three
currently return **501** with code `TRAINING_GENERATION_NOT_IMPLEMENTED`. They do
not return fake schedules, persist data or call AI. Invalid requests return 400,
unauthenticated requests 401, and missing athlete profiles 404. OpenAPI describes
future 200 draft responses without claiming that generation is available now.

```bash
curl -i -X POST http://localhost:8080/api/v1/training/plans/generate \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"goalId":"00000000-0000-0000-0000-000000000002","startDate":"2026-11-03","endDate":"2026-12-03"}'
```

Future adaptation is reserved by `TrainingPlanner.adaptPlan` and
`TrainingAdaptationContext` (plan ID, expected version and effective date).
There is deliberately no adaptation HTTP endpoint yet. An implementation must
authorize the current plan, obtain typed activity/recovery/readiness inputs,
evaluate approved rules and use optimistic locking before persisting a revision.
These are extension requirements, not implemented behavior. AI remains limited
to explaining recommendations produced by business engines.

## Cache

`CacheService` exposes get/put/evict. Configuration selects `NoOpCacheService`
unless `CACHE_ENABLED=true`; `RedisCacheService` catches Redis operation failures
and treats them as cache misses / best-effort writes. Redis is excluded from
readiness health, and operations use a short timeout.

Keys: `runcoach:v1:{dashboard|profile|readiness|nutrition|training-plan}:athlete:{uuid}`.
Positive TTLs are mandatory. Do not use cache as the source of truth or store
tokens/payment data there. No application service is cached yet. Failed
evictions may leave data until TTL expiry; choose TTLs/invalidation and revisit
consistency before caching sensitive or safety-relevant recommendations.

## AI Boundary

```text
Business engines -> persisted recommendations -> CoachContext
  -> AiCoachService -> PromptBuilder + KnowledgeBaseService -> OpenAiClient
```

The arrow is the intended future flow, not an implemented pipeline.
`TrainingPlanEngine` exposes unimplemented planning contracts; `ReadinessEngine`
and `NutritionEngine` remain marker ports. `CoachContext` accepts engine-labelled,
versioned recommendations, not raw health histories. `AiCoachService.explain`
currently throws HTTP 501 and never invokes OpenAI. No AI REST route is exposed.
The HTTP client contract targets OpenAI Responses; it is not wired to API keys.

`PromptBuilder` defines explanation-only instructions. `KnowledgeBaseService`
returns source-attributed snippets; its current adapter returns an empty list.
A future RAG adapter can add retrieval without giving AI write access or plan
authority. Prompt text alone is not a safety boundary: enforce context
authorization, minimized data, approved-output validation, cost/rate limits,
timeouts and retention before enabling remote AI. `runcoach.ai.enabled=false`
reserves a future toggle; changing it does not activate the stub.

## Flutter

```bash
cd frontend
flutter pub get
dart run build_runner build --delete-conflicting-outputs
dart format lib test
flutter analyze
flutter test
flutter run -d chrome --web-port 3000 --dart-define=API_BASE_URL=http://localhost:8080
```

Freezed/JSON generated files are produced locally, not hand-written. Commit the
resolved `pubspec.lock` once Flutter is available to make application builds
repeatable. Dependencies use compatible version ranges until then.

Each feature starts with presentation; profile demonstrates domain, data,
repository abstraction, Freezed DTOs and Riverpod async state. Grow other
features into domain/data/application/presentation layers only when needed.
GoRouter provides all ten feature routes. Authentication/onboarding routes are
placeholders, not functional flows or route guards. The dashboard is the initial
screen; no business or fake metrics are displayed.

Dio adds an access token from an injectable `TokenStore`. Its default in-memory
adapter is for scaffolding only: implement OIDC authorization code + PKCE, secure
platform token storage/session handling and route guards before real accounts.
The example profile provider is not bound to the placeholder page yet.

The web host is included. Generate native hosts when Flutter is installed:

```bash
flutter create --platforms=android,ios --org io.runcoach --project-name runcoach --empty .
```

Review generated files without replacing the existing Dart sources/tests. iOS
builds require macOS/Xcode. Android emulators use `http://10.0.2.2:8080` as the API
base URL; physical devices require a reachable development address and explicit
development-only transport configuration. Use HTTPS for production.

## Deployment

```bash
docker compose --env-file .env -f deployment/docker/docker-compose.yml --profile web up --build -d
```

Web: `http://localhost:3000`; backend: `http://localhost:8080`. Nginx serves the
Flutter release and proxies `/api/` to `backend:8080`, preserving JWT headers.
The container Flutter build uses a same-origin API URL; standalone development
uses the compile-time `API_BASE_URL` override. TLS terminates at the ingress or
an external proxy; the local Compose stack is not internet-facing production.

Kubernetes templates expect externally managed PostgreSQL and optional Redis.
Build/push both images to your registry, replace image names and example host
values, provision `runcoach-secrets` with `DB_PASSWORD` in namespace `runcoach`,
and provision the `runcoach-tls` certificate secret separately. Do not put secret
values in manifests or ConfigMaps. Then:

```bash
kubectl apply -f deployment/kubernetes/namespace.yaml
# Provision external secrets, database, TLS and edit config/image references first.
kubectl apply -f deployment/kubernetes/
```

The ingress expects an installed ingress-nginx controller. The templates provide
non-root containers, resource budgets, writable `/tmp`, and probes. External
database backups, secret management, network policies, observability, production
migration jobs, PodDisruptionBudgets and autoscaling remain deployment work.

Helm is an alternative **backend-only** installation, not an additional release
to apply on top of the raw backend manifests:

```bash
helm lint deployment/helm/runcoach
helm upgrade --install runcoach deployment/helm/runcoach --namespace runcoach --create-namespace
```

Supply environment-specific values and an existing database secret first. The
chart creates a backend service, not the web tier/ingress; add those separately
and align Nginx's backend upstream if deploying the chart with the web image.

## Development

```bash
mvn -f backend/pom.xml verify
mvn -f backend/pom.xml '-Dtest=Training*Test' test
mvn -f backend/pom.xml '-Dtest=Billing*Test' test
# Docker or an isolated external PostgreSQL database is required:
mvn -f backend/pom.xml -Ppostgres-it verify
bash scripts/build-all.sh
bash scripts/create-module.sh example
bash scripts/clean-all.sh
```

Athlete unit tests cover DTO validation, named time zones, protected-field
mapping, creation, duplicate-subject races, updates and optimistic-lock conflicts.
Quarkus HTTP tests exercise authentication requirements, request validation,
response shapes/headers, creation and update status codes using a mocked
application service. Their test profile disables JDBC/ORM/Flyway; no external
database or Redis is required. Test-only JWT identities do not verify signatures
or introduce a production bypass. Other unit tests cover cache outage behavior,
key namespacing and the unimplemented AI boundary.
Training unit tests cover context ownership, seven-day windows, invalid dates,
request/entity constraints and the unavailable planning contracts. Training HTTP
tests reuse the database-free test profile and mock only AthleteService, exercising
the real training service, planner adapter and 501 exception mapper. They do not
prove goal authorization, rule evaluation, generation or training persistence,
which are intentionally not implemented.
Billing tests cover trial eligibility, expiry, lifecycle policies, receipt
matching, idempotent settlement and JWT/ADMIN HTTP security. The opt-in
`BillingPostgresIT` suite uses PostgreSQL and test-only signed gateway/clock
adapters for concurrent trials and checkout -> verification -> activation ->
invoice flows. It is declared in the dedicated PostgreSQL CI job. Locally it
was blocked by unavailable Docker; its database assertions are not claimed as passing.
Flutter starter tests cover compact/wide navigation and the in-memory token store.
CI declares backend and Flutter build/test jobs. Database migration execution,
real PostgreSQL uniqueness/locking/rollback behavior, JWT signature verification,
end-to-end flows and container/cluster smoke tests require their real dependencies
and are not claimed by these test results.

All helper scripts resolve the repository root independently of the caller's
directory. Cleaning removes build output, not PostgreSQL volumes or user data.