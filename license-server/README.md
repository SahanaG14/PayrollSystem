# Payroll System Licensing Service

This licensing service issues **lifetime** licenses for Payroll System. Each license is verified online against Cloudflare D1 and is bound to the customer's machine(s) up to the allowed seat limit.

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

## 🔄 Moving a Customer to a Replacement Computer

If a customer replaces their computer or needs their activation reset:

1. Revoke their previous activation:
```bash
curl -X POST https://payroll-license-api.adityapdixit.workers.dev/v1/admin/activations/<ACTIVATION_ID>/revoke \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET"
```
2. Have the customer launch Payroll System on their new machine and enter their existing license key to activate it.

---

## 🔒 Security Best Practices

- The server stores only cryptographic **SHA-256 hashes** of license keys.
- **Never share or commit `ADMIN_SECRET`** into source control or include it in client desktop builds.
- The desktop app uses `LicenseService.java` to validate licenses at launch.

