# Arvexa Android Capture (device validation required)

Native Java Android companion, Android 8+ (API 26). The website cannot receive phone SMS itself.
This app listens to new SMS only after explicit disclosure, sign-in, and RECEIVE_SMS permission.
It does not request READ_SMS, notification-listener access, accessibility, contacts, or SEND_SMS.

## Build

Open Android/ in Android Studio, use JDK 17 and install Android SDK 35.
The checked-in Gradle 8.9 wrapper pins the distribution SHA-256 checksum.
Alternatively from a terminal (use gradlew.bat on Windows):

```sh
cd Android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The GitHub Actions Android job publishes a debug APK artifact. A debug APK is for testing,
not a production Play Store release. No signing keystore or release credentials are included.

## Connect once

1. Deploy the backend and frontend from the same feature branch/version. Back up the database first.
2. On the website, open Automatic Capture and map each supported bank + last four account digits.
3. Install the debug APK on your Android test phone. Open Arvexa Capture, verify the backend URL,
   sign in and read/accept the disclosure. The URL must be HTTPS and end with /api.
4. Approve SMS permission. New supported SMS queues automatically, even when the activity is closed.
5. Check imported entries, review items and last device contact on the website.

The login JWT is exchanged for a scoped 90-day device token; the password/JWT are not persisted.
Reconnecting the same account/server preserves queued alerts; switching with pending data requires explicit disconnect. Device tokens and queued extracted fields use Android Keystore AES-GCM encryption. Android backup
is disabled. Revoke devices on the website. Reconnect after expiry. The app never logs or uploads raw SMS.

## Coverage and limitations

The source parser is shared with backend tests. It accepts known Indian bank sender IDs and conservative
English credit/debit templates containing an INR/Rs/₹ amount. OTPs, marketing without transaction verbs,
pending/failed alerts, multiple conflicting amounts/directions and unsupported sender IDs are ignored.
No historical inbox scan or MMS/RCS/UPI-app notification capture is included. Unknown templates can
be missed; this is not a guaranteed complete or bank-verified feed. Bank sender IDs are not proof of authenticity.
SMS timestamp is used as the recorded transaction time in Asia/Kolkata, not a verified bank posting time.

WorkManager retries after network/server failures. Android can delay background work; force-stopping,
revoking permission, restricting battery/background activity or disabling SMS delivery can prevent capture.
There is no promise of instant or exhaustive delivery. The device screen shows queued count and sync state.
A server validation rejection retains the item and requires attention; disconnect explicitly deletes that queue.

## Distribution

Google Play restricts SMS permissions. Money-management exceptions require declaration and approval.
This source/build does not imply approval. Do not add notification scraping to bypass SMS policy.
Test installability and SMS permission behavior on target devices before deciding a distribution route.

## Required physical-device checks

- Permission allowed/denied/revoked; capture on/off; reboot and activity closed; force-stop behavior.
- Multipart alerts; SIM variants; real supported-bank formats; no personal/OTP messages uploaded.
- Network offline then online; server unavailable; duplicate delivery and retries; app process killed after enqueue.
- Device revocation/expiry, account switch and disconnect; queue never delivered to another account.
- Inspect encrypted local storage and backend payloads on a disposable test account.
