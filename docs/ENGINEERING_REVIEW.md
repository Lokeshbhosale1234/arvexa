# Arvexa engineering review

Reviewed 26 November 2026 from the uploaded source archive. Target: React + Java/Spring Boot full-stack roles.
This review is source-based; it is not a penetration test or a production deployment certification.

## Assessment

The application has more substance than a static dashboard: JWT authentication, ownership-aware
transaction deletion, JPA filtering, budgets, and a substantial deterministic insights engine exist.
However, the original dashboard mixed real records with fictional recommendation fallbacks, displayed an
invented financial score on errors, and called all-time totals monthly totals. Those issues undermine trust.
The root README also used the old Zentriq name and listed authentication as a future feature despite its implementation.

A hiring reviewer should be able to run this project, inspect the security boundaries, understand the
financial definitions and reproduce an interesting workflow. Visual effects alone do not establish that.

## Changes included in this package

| Area | Original behavior | Updated behavior |
| --- | --- | --- |
| Dashboard totals | All-time totals labelled monthly | Explicit total/all-recorded labels |
| Health score | Missing score became 72 | Explicit unavailable state on dashboard and insights page |
| Recommendation list | Fictional personal spending presented as Live AI | Actual insights or an explicit empty/error state; data-source label |
| Status badge | Unconditional All systems normal | Actual financial-data request state |
| CSV export | No export on transactions page | Export displayed results with quoting, UTF-8 and formula neutralization |
| Date filtering | Browser converted dates to UTC while API accepts LocalDateTime | Send matching local date-time boundaries; retain full end-of-day precision |
| Type filter | Invalid type silently removed the filter | Return HTTP 400 |
| Amount input | Fractional paise could reach database rounding | Validate at most two decimals and column-compatible magnitude |
| Error responses | Transaction endpoints returned internal exception text | Safe server errors, structured field validation, distinct 400/404/500 |
| Deployment | API URL repeated in source | Shared build-time environment variable with compatibility fallback |
| Health | No purpose-built public health endpoint | Actuator aggregate health endpoint without details |
| Loading | All chart pages in initial bundle | Lazy-loaded authenticated pages |
| Automation | Broken lint configuration; no included regression tests | Working lint, export tests, backend unit tests and CI workflow |

No schema migration or paid service is required by these changes. Production has not been changed.

## Highest-impact remaining work

### 1. Make the financial definitions rigorous

Current analytics aggregate recorded transactions regardless of status. Decide and implement a consistent
posted-balance rule: COMPLETED entries affect posted balances; PENDING entries are separate commitments;
FAILED and CANCELLED entries do not represent posted cash flow. Apply that policy consistently across
analytics, budget spending, insights and charts. Introduce an injected Clock and an explicit account timezone
before implementing current-month comparisons. Add PostgreSQL-backed repository tests at month boundaries.

Acceptance: the same fixture yields matching totals on every screen, including pending/cancelled entries,
zero income, refunds and month changes. No fixture is silently substituted for missing user data.

### 2. Build the signature workflow: statement import and reconciliation

Let a reviewer upload a documented CSV format, preview parsed rows, correct errors and categories,
see duplicate candidates, then confirm an atomic import. Track import batches and support undo.
Do not claim arbitrary bank-statement support without parsers for those formats.

Acceptance: retrying a confirmed import cannot duplicate records; another user cannot access the batch;
malformed rows are actionable; export/reimport semantics and deduplication rules are documented and tested.
This creates useful interview topics: idempotency, validation, transactions, ownership and bulk persistence.

### 3. Remove friction from the recruiter demo

Provide an isolated, explicitly labelled sample-data workspace with a reset path. Do not share a mutable
account that lets strangers overwrite each other's data. Offer a short guided flow: inspect spending,
create a budget, import a statement, and explain an insight. Record a 60–90 second walkthrough.

Acceptance: the reviewer can understand the app without registering; all sample data is labelled and isolated;
no real user's records appear; the live links and a repeatable demo remain available.

### 4. Strengthen operational and security evidence

Add PostgreSQL integration tests for ownership and aggregates, end-to-end browser tests, a documented
API contract, rate limiting for authentication and billable AI requests, dependency updates with regression
checks, restricted configurable CORS, and versioned database migrations. Audit token storage and expiry.
The current application permits broad CORS and stores the bearer token in localStorage.

Acceptance: a fresh clone has reproducible checks; CI actually passes; backups and deployment rollback are
explained; health checks detect database failure; authentication and AI failures remain understandable.

### 5. Make optional AI bounded and explainable

The existing Gemini client has no explicit connection/read timeouts, returns error strings as successful
content, sends transaction titles, and has no rate limit. Make the API key optional, bound request sizes,
prefer aggregated inputs, distinguish upstream failures, and add fallback to deterministic insights.
Show what data generated an insight. Never describe rule-based insights as a live model response.

Acceptance: the core app works without a Gemini key; external timeouts cannot hang requests indefinitely;
quota failure does not look like successful analysis; input size and cost are bounded and tested.

## Suggested interview narrative

Explain the user problem, show one complete workflow, then explain the architecture and one real bug:
for example, why a missing health score must remain unavailable instead of being replaced by 72.
Show ownership tests and a failed/successful CI run. Discuss all-time versus monthly reporting,
BigDecimal precision, idempotent imports and the cost/availability tradeoffs of free hosting.
Only present the import workflow or other roadmap items as completed after implementing and verifying them.

## Release checklist

- Run frontend lint, tests and production build.
- Run backend tests and build using Java 17.
- Check ownership behavior with two disposable accounts against PostgreSQL.
- Verify failure states with database and AI unavailable.
- Verify desktop/mobile layout and keyboard access in a browser.
- Deploy to the existing services, inspect health and retest the main workflows.
- Update the README with live links, screenshots and the actual verified results.
