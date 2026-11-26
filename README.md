# Arvexa

Personal finance application built with React, Java 17, Spring Boot and PostgreSQL.
Track transactions, manage category budgets, inspect spending trends and request optional Gemini-generated explanations.

## Automatic capture (Android companion)

Automatic tracking now has an Android companion in `Android/` and a website page at `/capture`.
After account mapping and explicit SMS permission, supported new bank alerts sync automatically.
Uncertain candidates go to review; duplicate alerts are idempotent. Raw SMS and OTPs are not uploaded.
This requires installing the companion; the website alone cannot read phone SMS.

See [setup, architecture, limitations and rollout checks](docs/AUTOMATIC_CAPTURE.md).
Download the debug APK artifact from a successful Android GitHub Actions job for device testing.
Physical-device validation and Google Play permission approval remain separate requirements.

## What works

- Registration and login with BCrypt password hashing and JWT authentication.
- Transactions scoped to the authenticated user, with search and date/amount/category filters.
- Category budgets and spending analytics.
- Deterministic spending insights and financial health scoring; separate optional Gemini analysis.
- CSV export of the transaction results currently displayed, including spreadsheet-formula protection.
- Explicit unavailable states instead of invented recommendations or financial scores.
- Public aggregate backend health endpoint: `GET /actuator/health`.

This is a portfolio application, not a bank or payment processor. The Payments screen is a placeholder.
Savings-goal and bill widgets in the source are labelled demo components and are not persisted features.

## Architecture

The React application calls one Axios client. That client attaches a bearer token.
Spring Security validates it, controllers validate requests, services apply business rules,
and JPA repositories scope data to the authenticated account before querying PostgreSQL.
The rule-based insights engine is separate from the Gemini HTTP integration.

See [the engineering review](docs/ENGINEERING_REVIEW.md) for verified findings, limitations and prioritized next work.

## Local setup

Requirements: Node.js 22, a Java 17 JDK, Maven 3.9+, and PostgreSQL.

### Backend

Set these environment variables in your terminal or deployment provider:

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC PostgreSQL URL, including SSL settings required by your provider |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `JWT_SECRET` | Strong signing secret compatible with the existing JWT configuration |
| `GEMINI_API_KEY` | Optional analysis integration key; use an empty value when not configured |
| `PORT` | Optional HTTP port, defaults to 8080 |

```sh
cd Backend
mvn spring-boot:run
```

The existing application uses `spring.jpa.hibernate.ddl-auto=update`. Back up the database
before deployment; versioned migrations are not included in this update.

### Frontend

Copy `Frontend/.env.example` to `Frontend/.env.local` and set `VITE_API_BASE_URL`.

```sh
cd Frontend
npm ci
npm run dev
```

Vite configuration is embedded at build time. Never put database passwords, JWT signing secrets,
or Gemini keys in frontend variables. The compatibility fallback remains the existing Render API URL.

## Checks

```sh
cd Frontend
npm run lint
npm test
npm run build
```

```sh
cd Backend
mvn verify
```

GitHub Actions runs both sets of checks on pushes and pull requests. A workflow file is not evidence
that a run has passed; inspect the Actions results after pushing.

## API overview

| Method | Route | Access |
| --- | --- | --- |
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET / POST | `/api/transactions` | Authenticated; own data |
| DELETE | `/api/transactions/{id}` | Authenticated; owner only |
| GET | `/api/transactions/analytics` | Authenticated; own recorded transactions |
| GET / POST | `/api/budgets` | Authenticated |
| PUT / DELETE | `/api/budgets/{id}` | Authenticated |
| GET | `/api/budgets/analytics` | Authenticated |
| GET | `/api/insights/summary` | Authenticated; rule-based insights |
| GET | `/api/ai/insights` | Authenticated; external Gemini request |
| GET | `/actuator/health` | Public; aggregate status only |

Transaction amounts accept up to 13 integer digits and two decimal places. Invalid transaction
filters return HTTP 400; missing or non-owned transaction IDs return HTTP 404 on deletion.
Dashboard income/expense cards cover all completed non-transfer transactions, not just the current month. Recorded transaction counts include every status.

## Deployment

1. Merge the updated source into your repository and inspect GitHub Actions results.
2. In Vercel, keep `Frontend` as the application root. Set `VITE_API_BASE_URL` to the Render API URL ending in `/api`, then rebuild.
3. In Render, keep the existing backend service, Docker setup and secrets. Deploy the new backend after tests pass.
4. Configure `/actuator/health` as the backend health-check path if your hosting configuration supports it.
5. Test registration, login, transaction creation/deletion, budgets, insights and CSV export using a disposable account.

If login fails after inactivity, inspect backend logs and database availability separately.
A timeout does not prove the server is merely waking up.

## Honest project claims

Describe implemented behavior and measured results. Do not claim payment processing, production-grade
security, real-time monitoring, an independently validated financial score, or guaranteed job outcomes.
