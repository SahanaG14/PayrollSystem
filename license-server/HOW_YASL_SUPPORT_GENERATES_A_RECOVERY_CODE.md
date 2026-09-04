# How YASL Support Generates a Recovery Code

## Before the first recovery

Use a support-only computer. Keep `license-server/support-recovery.mjs` and the `.env` file outside the customer installer and outside shared folders. The `.env` file needs only:

```text
ADMIN_SECRET=your Cloudflare Workers administrator secret
LICENSE_SERVER_URL=https://your-worker.workers.dev
```

Deploy the Worker recovery changes once:

```powershell
cd C:\path\to\PayrollSystem\license-server
npx wrangler d1 execute payroll-licenses --remote --file=migrations/002_recovery_codes.sql
npx wrangler d1 execute payroll-licenses --remote --file=migrations/003_support_audit_log.sql
npx wrangler secret put RECOVERY_SECRET
npx wrangler deploy
```

`RECOVERY_SECRET` is the signing key. Generate a long random value, store it only in Cloudflare Workers Secrets and your company password manager, and never place it in the Payroll desktop application or `.env`.

## Generate a code

1. Ask the verified client to open **Login → Forgot Username or Password?**.
2. The client sends the displayed Support ID, for example `YASL-A7K9-P2M4-Q7W8`. They do not send their database or old password.
3. Open PowerShell on the support-only computer:

```powershell
cd C:\path\to\PayrollSystem\license-server
node .\support-recovery.mjs generate YASL-A7K9-P2M4-Q7W8 15
```

4. The final number is expiry minutes: choose 5–60. Fifteen minutes is recommended.
5. The generated code is a support-side authorization record. It is bound to that Support ID, expires automatically, and can be used once. Do not send it to the client.

Sample output (illustrative only):

```text
Recovery code generated
Support ID : YASL-A7K9-P2M4-Q7W8
Recovery ID: exampleRecoveryId
Expires    : 02/09/2026, 10:45:00 pm

Support-only authorization code (do not send to client):
example.one-time.recovery-code
```

Use a verified support channel. Suggested message:

> Your credential reset has been authorised. In Payroll, select **Forgot Username or Password?** and choose **Check for Credential Reset** within 15 minutes. Then log in manually with the temporary credentials provided by YASL Support and change them immediately from Settings.

Do not include the client's previous password, database, recovery ID, `ADMIN_SECRET`, or `RECOVERY_SECRET` in a message.

## Client completion

1. Client selects **Check for Credential Reset** in **Credential Recovery**.
2. The desktop app sends its internally held full installation hash and license key over HTTPS to the Worker.
3. The Worker finds the pending signed authorization for that same active installation, checks expiry and unused status, then consumes it atomically.
4. Only after approval, the desktop app resets local credentials. It does not log in automatically or alter payroll data.
5. Client manually returns to Login and uses `User` / `Welcome@123`.
6. They use `123456` for Master Data, then change both credential sets in **Settings → Password management**.

Incorrect, expired, modified, or already-used codes are rejected. The client remains at the recovery screen and no credentials or payroll records are changed.

## Revoke an unused code

```powershell
node .\support-recovery.mjs revoke RECOVERY_ID
```

The `RECOVERY_ID` is printed when the code is generated. Revocation marks it used server-side; it cannot later be redeemed.

## Audit and key recovery

Generation and revocation are recorded in the D1 `support_audit_log` table. Recovery-code metadata is in `recovery_codes`; only a SHA-256 hash of a code is stored—never the readable code or client passwords.

Back up `RECOVERY_SECRET` in a restricted company password manager with two authorised custodians. If it is lost, set a new Worker secret and redeploy. Previously generated unused codes will no longer verify; revoke/reissue them. Existing licenses and payroll databases are unaffected.

## Safe test

Use a dedicated test license and test Windows account, never a production customer. Activate the test copy, obtain its Support ID, generate a five-minute code, redeem it once, then attempt redemption again. It must fail. Generate another five-minute code, wait until expiry, then confirm it fails. A changed character must also fail.

## Plain-language verification

The Support ID tells YASL which installation needs help. The Worker creates a temporary code for that installation only. When the code is entered, the Worker checks that it is genuine, for the same installation, unexpired, and unused. The Worker then marks it used forever before the application resets credentials.

## Technical verification

The Worker uses `RECOVERY_SECRET` to HMAC-sign `recoveryId.randomToken.expiry.machineId`. It stores only SHA-256(code), activation ID, expiry, and used status in D1. The app does not contain this secret or a bypass key; it sends the code to the Worker through HTTPS. D1 conditionally updates `used_at`, preventing replay even under concurrent requests.
