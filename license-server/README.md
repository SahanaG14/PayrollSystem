# Yashasvi Accounting Solutions LLP Licensing Service

This licensing service issues **lifetime** licenses for Yashasvi Accounting Solutions LLP. Each license is verified online against Cloudflare D1 and is bound to the customer's machine(s) up to the allowed seat limit.

- **Production API Endpoint**: `https://payroll-license-api.adityapdixit.workers.dev`

---

## 🔑 How to Generate a New License Key

### Quick CLI Method (Recommended)

Make sure you have Node.js installed and `ADMIN_SECRET` configured in `license-server/.env`.

#### 1. Generate a single-seat license (default: 1 computer)
```bash
node license-server/create-license.mjs 1
```

**Example Output:**
```text
🎉 License Key Generated & Registered:
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Key       : PAY-5S53-VCR3-KTY9-TJWN
   Max Seats : 1
   Server    : https://payroll-license-api.adityapdixit.workers.dev
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### 2. Generate a multi-seat license (e.g., 5 computers)
```bash
node license-server/create-license.mjs 5
```

#### 3. Register a specific custom license key
```bash
node license-server/create-license.mjs 1 PAY-COMPANY-2026-X89
```

---

### Manual cURL Method

You can also create a license directly by making a POST request to the API:

```bash
curl -X POST https://payroll-license-api.adityapdixit.workers.dev/v1/admin/licenses \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"licenseKey":"PAY-CUSTOM-KEY-HERE","maxSeats":1}'
```

---

## 🚫 How to Revoke / Manage Licenses

You can manage license states using the [`license-server/revoke-license.mjs`](file:///Users/adityadixit/Desktop/PayrollSystem/license-server/revoke-license.mjs) CLI tool:

### 1. Disable / Revoke an Entire License Key (e.g., Refund or Chargeback)
Permanently disables the license key so no machine can use it:
```bash
node license-server/revoke-license.mjs --key PAY-XXXX-XXXX-XXXX-XXXX
```

### 2. Reset Activations for a Customer (e.g., New Computer / Replaced PC)
Clears all active computer bindings for a license key so the customer can activate their new machine without needing a new key:
```bash
node license-server/revoke-license.mjs --reset PAY-XXXX-XXXX-XXXX-XXXX
```

### 3. Revoke a Specific Computer Activation by ID
```bash
node license-server/revoke-license.mjs --activation <ACTIVATION_ID>
```

### 4. Re-Enable a Disabled License Key
```bash
node license-server/revoke-license.mjs --enable PAY-XXXX-XXXX-XXXX-XXXX
```
## Credential recovery

Set the server-only signing secret once (never add it to the desktop application):
```bash
npx wrangler secret put RECOVERY_SECRET
```
Apply the recovery-code migration to an already-deployed D1 database:
```bash
npx wrangler d1 execute payroll-licenses --remote --file=migrations/002_recovery_codes.sql
```
After verifying a customer's Installation ID, support can issue one short-lived, one-time code:
```bash
curl -X POST https://payroll-license-api.adityapdixit.workers.dev/v1/admin/recovery-codes \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"supportId":"YASL-A7K9-P2M4-Q7W8","expiresMinutes":15}'
```
The client submits that code in **Forgot Username or Password?**. The Worker consumes it once and authorizes a local credential reset without touching payroll data.

---

## 🔒 Security Best Practices

- The server stores only cryptographic **SHA-256 hashes** of license keys.
- **Never share or commit `ADMIN_SECRET`** into source control or include it in client desktop builds.
- **Never share or commit `RECOVERY_SECRET`**; it signs server-issued recovery codes and remains only in Cloudflare Workers secrets.
- The desktop app uses `LicenseService.java` to validate licenses at launch.


