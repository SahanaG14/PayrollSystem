# Payroll System — Complete Licensing & Key Management Guide

This document contains instructions, command references, and practical scenarios for issuing, validating, resetting, and revoking lifetime licenses for Payroll System.

---

## 🌐 Server Details & Architecture

- **Production Cloudflare Endpoint**: `https://payroll-license-api.adityapdixit.workers.dev`
- **Database Engine**: Cloudflare D1 (SQLite at the edge in Singapore/APAC)
- **Key Storage**: High-security SHA-256 one-way hashing (raw keys are never stored on the server)
- **Machine Identification**: SHA-256 hardware hash bound to OS platform, architecture, and network interface MAC addresses
- **Validation Schedule**: Verified online against Cloudflare D1 upon every application startup

---

## 🛠️ CLI Quick Reference Cheat Sheet

All commands are run from the project root using Node.js:

| Action / Scenario | Command |
| :--- | :--- |
| **Generate 1-Computer License** | `node license-server/create-license.mjs 1` |
| **Generate Multi-Seat License (e.g. 5 PCs)** | `node license-server/create-license.mjs 5` |
| **Register Custom Branded Key** | `node license-server/create-license.mjs 1 PAY-ACME-2026-X` |
| **View All Licenses & Active Machines** | `node license-server/revoke-license.mjs --list all` |
| **Reset Activations (Customer Moved to New PC)** | `node license-server/revoke-license.mjs --reset <KEY>` |
| **Disable / Revoke Key (Refund or Chargeback)** | `node license-server/revoke-license.mjs --key <KEY>` |
| **Re-Enable a Disabled Key** | `node license-server/revoke-license.mjs --enable <KEY>` |
| **Revoke a Specific Activation by ID** | `node license-server/revoke-license.mjs --activation <ID>` |

---

## 📖 Scenario-by-Scenario Guide

### Scenario 1: A New Customer Buys a Single License
A customer completes checkout on your website or storefront.

1. Run the key generator:
   ```bash
   node license-server/create-license.mjs 1
   ```
2. **Output:**
   ```text
   🎉 License Key Generated & Registered:
      Key       : PAY-5S53-VCR3-KTY9-TJWN
      Max Seats : 1
      Server    : https://payroll-license-api.adityapdixit.workers.dev
   ```
3. Deliver the generated `PAY-...` key to the customer via email or order receipt.
4. When the customer opens the app for the first time, they paste the key and click **Activate**. The app validates the key with the server, locks the license to their machine, and unlocks Payroll System.

---

### Scenario 2: A Business Buys a Multi-Computer Pack (e.g. 5 Seats)
A company buys a multi-seat license to install Payroll System on 5 accountants' laptops.

1. Generate a key with 5 seats:
   ```bash
   node license-server/create-license.mjs 5
   ```
2. Send the single key to the company.
3. Up to 5 separate machines can activate using this single key. The 6th machine attempting activation will receive an error: *"This license has reached its computer limit."*

---

### Scenario 3: Creating a Custom Branded Key
If you want to provide a personalized license key (e.g., enterprise contract or VIP client):

```bash
node license-server/create-license.mjs 10 PAY-ACME-ENTERPRISE-2026
```

---

### Scenario 4: Viewing All Active Licenses & Connected Computers
To check your active user base, see which computers are registered, and inspect seat usage:

```bash
node license-server/revoke-license.mjs --list all
```

**Example Output:**
```text
📋 Registered Licenses & Activations:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ID: 1 | Status: 🟢 ACTIVE | Max Seats: 2 | Created: 2026-08-31 16:31:44
  └─ Activation #1 | Machine: "Mac OS X / adityadixit" | Revoked: No
ID: 4 | Status: 🟢 ACTIVE | Max Seats: 1 | Created: 2026-08-31 16:47:38
  └─ Activation #3 | Machine: "Windows 11 / accountant" | Revoked: No
ID: 5 | Status: 🔴 DISABLED | Max Seats: 1 | Created: 2026-08-31 17:54:04
  └─ Activation #4 | Machine: "Windows 11 / user" | Revoked: No
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### Scenario 5: Customer Got a New Computer (Reset Computer Binding)
A customer upgraded their laptop or formatted their drive, and their license says *"This license has reached its computer limit."*

1. Run the reset command with their license key:
   ```bash
   node license-server/revoke-license.mjs --reset PAY-XXXX-XXXX-XXXX-XXXX
   ```
2. **Output:**
   ```text
   ✅ Reset complete. Revoked 1 active computer binding(s). Customer can now activate their new computer.
   ```
3. The customer simply opens Payroll System on their new PC, enters the same key, and activates successfully.

---

### Scenario 6: Customer Requested a Refund or Filed a Chargeback
If a customer cancels their purchase or issues a fraudulent payment, revoke their key so they can no longer use the software:

```bash
node license-server/revoke-license.mjs --key PAY-XXXX-XXXX-XXXX-XXXX
```
- **Immediate Effect**: Upon the next app launch (or validation check), the software displays *"License key is invalid or disabled"* and blocks access to the application.

---

### Scenario 7: Un-banning / Re-Enabling a License
If a billing dispute is resolved or a customer renews:

```bash
node license-server/revoke-license.mjs --enable PAY-XXXX-XXXX-XXXX-XXXX
```

---

### Scenario 8: Revoking a Specific Single Machine by Activation ID
If a 5-seat customer had 1 employee leave the company with their laptop:

1. List the activations using `node license-server/revoke-license.mjs --list all` to find the `Activation #ID`.
2. Revoke just that specific computer:
   ```bash
   node license-server/revoke-license.mjs --activation 3
   ```
3. Only that specific machine loses access; the remaining 4 seats remain active.

---

## 📡 Direct REST API / cURL Reference

If integrating license creation into an automated website checkout backend (Node, PHP, Python, Go, Stripe webhook, etc.):

### 1. Create License Key
```bash
curl -X POST https://payroll-license-api.adityapdixit.workers.dev/v1/admin/licenses \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"licenseKey":"PAY-GENERATED-KEY","maxSeats":1}'
```

### 2. Disable License Key
```bash
curl -X POST https://payroll-license-api.adityapdixit.workers.dev/v1/admin/licenses/disable \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"licenseKey":"PAY-GENERATED-KEY"}'
```

### 3. Reset Activations for a License Key
```bash
curl -X POST https://payroll-license-api.adityapdixit.workers.dev/v1/admin/licenses/revoke-activations \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"licenseKey":"PAY-GENERATED-KEY"}'
```

### 4. List All Licenses & Activations
```bash
curl -X GET https://payroll-license-api.adityapdixit.workers.dev/v1/admin/licenses \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET"
```

### 5. Health Check (Public)
```bash
curl -s https://payroll-license-api.adityapdixit.workers.dev/health
```

---

## 🔒 Security Summary

1. **`ADMIN_SECRET` Protection**:
   - Stored locally only in `license-server/.env` (gitignored).
   - Never included in client builds or shared with customers.
2. **Tamper Prevention**:
   - Modifying local `.properties` files does not bypass licensing because the app contacts the Cloudflare D1 endpoint on every launch.
   - Deleting or changing local state forces the application back to the activation prompt.
