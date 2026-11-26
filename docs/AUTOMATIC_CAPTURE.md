# Automatic bank SMS capture

## Product behavior

The Android companion captures new supported bank SMS after a one-time connection, disclosure,
SMS permission approval and account mapping. It does not read historical inbox messages or notifications.
The website remains the dashboard. No repeated copy/paste or approval is required for supported,
complete alerts matching a configured account. Uncertain entries remain in Needs review.

## Setup and deployment order

1. Back up PostgreSQL. Confirm the existing application database user can create tables, indexes,
   foreign keys and add columns. The existing `ddl-auto=update` creates three new capture tables
   and a nullable `transactions.internal_transfer` flag. No existing rows are dropped or rewritten.
2. Deploy the backend before installing the companion or using the new frontend.
3. Deploy the frontend. Open Automatic Capture and add a bank, last four digits and account label.
4. Download `arvexa-capture-debug-apk` from the successful Android Actions job, unzip and install
   `app-debug.apk` on a disposable Android test phone/account. This is not a signed release build.
5. Enter the trusted backend HTTPS URL ending in `/api`, sign in and grant SMS access after reading
   the disclosure. Leave capture enabled. Verify supported incoming SMS appears on the website.
6. Run the physical-device checklist in Android/README.md before merging/deploying to general users.

The website never accesses SMS itself. No Play Store approval is implied by a successful APK build.
The Android SMS money-management permission exception requires Google Play review and approval.

## Trust boundaries

- Ordinary website routes and device enrollment/revocation require the existing authenticated JWT.
- Enrollment returns a 256-bit random device token once. Only its SHA-256 hash is stored server-side.
- Tokens expire in 90 days, can be revoked, and only authorize the ingest/heartbeat service methods.
  They do not authorize general account reads, transaction listing, device enrollment or deletion.
- Owner identity is derived from the device credential, never an employee/user/account ID in an alert.
- Accounts and review operations are always scoped to the authenticated owner.
- Android parses on device. Raw message bodies, full account numbers and OTPs are not uploaded or persisted.
  Extracted amounts, bank codes, account endings, references, direction and SMS timestamps are synced.
- The device stores its scoped token and extracted outbox fields using Android Keystore AES-GCM;
  backup is disabled. The login password and JWT are not persisted.
- SMS is unverified input, not proof of a bank transaction. Bank sender IDs can be spoofed.
  There is no transfer of real money or payment authorization.

## Import rules

A conservative, shared Java parser recognizes allowlisted Indian bank sender IDs and supported English
credit/debit templates. It refuses OTP, failed/pending/scheduled/request messages, conflicting directions,
conflicting transaction amounts, malformed amounts, unsupported senders, and ambiguous account numbers.

Complete alerts with mapped bank + account ending + reference import automatically. A missing reference
or missing mapping holds the extracted candidate for review without affecting totals. Adding an account
also posts complete held candidates matching that account. Ambiguous parsing may be ignored entirely;
this initial version does not claim exhaustive template coverage.

For referenced alerts the deduplication identity includes owner, bank, account ending, direction, bank
reference and exact two-decimal amount. Without reference/account ending, an event fingerprint prevents
retries from duplicating the same message, but cross-message duplicate detection cannot be guaranteed.
Such candidates need review. Different banks/accounts with the same last four digits stay separate;
two accounts at the same bank sharing the same last four digits cannot be distinguished in this version.

Posting, deduplication and reconciliation run in a transaction while holding a per-owner database lock.
A database uniqueness constraint additionally enforces deduplication. Concurrent retry tests exercise
this boundary on real PostgreSQL. Deleting an imported transaction retains its capture identity so a
phone retry cannot silently recreate it.

## Balances, transfers and time

The account card displays **net captured movement**, not an available bank balance. It excludes the opening
balance, manual records and missing/unsupported alerts. No bank balance is inferred from a partial SMS history.

A debit and credit sharing a reference and amount in different mapped accounts within three days are
marked as an internal transfer. Both account movements remain in history; both transactions are excluded
from income, expense, budgets and model inputs. Until both legs arrive, the first leg can temporarily appear
as income/spending. Transfers without matching references or missing legs cannot be auto-reconciled.

Posted analytics now include only COMPLETED non-transfer records; failed, cancelled and pending records
remain visible in transaction history. Transaction counts still count recorded entries of every status.
SMS timestamp is recorded in Asia/Kolkata as the transaction time. It is not a verified bank posting date.
Capture history displays the original timestamp using the viewer's browser timezone.

## Background behavior and recovery

The receiver assembles multipart SMS, parses locally, persists encrypted extracted fields, then requests
WorkManager synchronization. Network/server errors retry with backoff, and periodic work provides another
sync opportunity. HTTP 401/403 pauses capture and requests reconnect; a rejected payload is retained locally and marked for attention without blocking other alerts. Disconnect deletes the local queue explicitly. Device revocation immediately rejects
new requests; an already in-flight authorized request may finish.

Android battery restrictions, force-stop, permission revocation, network outages and unsupported formats
can delay or prevent capture. Historical messages are not recovered automatically. The website polls records
while visible (30 seconds for dashboard transactions/analytics; 15 seconds for capture status). Rule-based insights refresh without repeatedly requesting Gemini. This is not a
promise of instantaneous delivery, server push or a complete bank feed.

## Verification

CI runs frontend lint/render/utility checks and build, backend unit plus PostgreSQL integration tests,
and Android lint/unit tests/debug APK assembly. It does not yet simulate an actual device receiving an SMS.
The parser source is shared directly with the Android build; there are no independent Android regex copies.
A successful integration test does not establish that every real bank template works.

Before rollout, test real-device permission grant/denial/revocation, background/reboot behavior, multipart
messages, offline retries, duplicate alerts, account switching, device revocation, and privacy of stored/uploaded
data. Use sanitized sample messages when expanding fixtures. Do not commit actual personal SMS or credentials.
