# Verification results

26 November 2026. These results apply to the supplied source package, not the live deployment.

| Check | Result |
| --- | --- |
| Frontend dependency install | Passed with npm ci |
| Frontend lint | Passed after repairing the existing lint setup |
| CSV unit tests | 4 passed |
| Server-rendered component regression tests | 4 passed |
| Vite production build | Passed |
| Backend production/test source compilation | Passed with Java 17 using Maven test-compile |
| Backend unit-test execution | Blocked by Maven Surefire dependency resolution in this environment; not claimed as passed |
| Browser interaction checks | Blocked: available browser launch cannot create required sockets in this environment |
| PostgreSQL integration tests | Not included or run |
| Live Vercel/Render deployment | Not performed |

The component tests check output rendering; they do not replace browser interaction or accessibility tests.
Backend tests cover authenticated ownership on create/list/delete, invalid filters, empty-account analytics,
and fractional-paise rejection. GitHub Actions is configured to execute them, but no remote run is claimed.

The initial JavaScript entry bundle changed from approximately 754 KB to 254 KB uncompressed
(211 KB to 81 KB gzip) after route splitting. Authenticated chart routes still load additional chunks.
This is a build artifact measurement, not a load-time, Core Web Vitals or FPS benchmark.

Before deploying, run mvn verify in Backend, inspect CI, and complete the release checklist in
ENGINEERING_REVIEW.md. A dependency vulnerability audit and production data migration rehearsal remain outstanding.
