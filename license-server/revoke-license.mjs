#!/usr/bin/env node
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const envFile = path.join(__dirname, '.env');

let adminSecret = process.env.ADMIN_SECRET;
if (!adminSecret && fs.existsSync(envFile)) {
  const content = fs.readFileSync(envFile, 'utf8');
  const match = content.match(/ADMIN_SECRET=(.*)/);
  if (match) adminSecret = match[1].trim();
}

const workerUrl = process.env.LICENSE_SERVER_URL || "https://payroll-license-api.adityapdixit.workers.dev";

if (!adminSecret) {
  console.error("❌ ADMIN_SECRET is missing. Configure license-server/.env");
  process.exit(1);
}

async function api(path, method = 'POST', body = null) {
  const opts = {
    method,
    headers: {
      'Authorization': `Bearer ${adminSecret}`,
      'Content-Type': 'application/json'
    }
  };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${workerUrl}${path}`, opts);
  return { status: res.status, data: await res.json().catch(() => ({})) };
}

const args = process.argv.slice(2);
const command = args[0];
const target = args[1];

async function main() {
  if (!command || !target || command === '--help' || command === '-h') {
    console.log(`
📋 Payroll System License Revocation Tool

Usage:
  node license-server/revoke-license.mjs --key <LICENSE_KEY>
      -> Disables a license completely (e.g. for refund or chargeback).

  node license-server/revoke-license.mjs --reset <LICENSE_KEY>
      -> Revokes all computer activations for this key so the customer can activate a new machine.

  node license-server/revoke-license.mjs --activation <ACTIVATION_ID>
      -> Revokes a single specific computer activation ID.

  node license-server/revoke-license.mjs --enable <LICENSE_KEY>
      -> Re-enables a previously disabled license.
    `);
    process.exit(0);
  }

  if (command === '--key' || command === 'disable') {
    console.log(`Disabling license key: ${target}...`);
    const { status, data } = await api('/v1/admin/licenses/disable', 'POST', { licenseKey: target });
    if (status === 200 && data.disabled) {
      console.log(`✅ License key [${target}] has been DISABLED.`);
    } else {
      console.error(`❌ Failed to disable license:`, data);
    }
  } else if (command === '--reset' || command === 'reset') {
    console.log(`Resetting computer activations for key: ${target}...`);
    const { status, data } = await api('/v1/admin/licenses/revoke-activations', 'POST', { licenseKey: target });
    if (status === 200) {
      console.log(`✅ Reset complete. Revoked ${data.revokedCount || 0} active computer binding(s). Customer can now activate their new computer.`);
    } else {
      console.error(`❌ Failed to reset activations:`, data);
    }
  } else if (command === '--activation' || command === 'activation') {
    console.log(`Revoking activation ID: ${target}...`);
    const { status, data } = await api(`/v1/admin/activations/${target}/revoke`, 'POST');
    if (status === 200 && data.revoked) {
      console.log(`✅ Activation ID [${target}] has been REVOKED.`);
    } else {
      console.error(`❌ Failed to revoke activation:`, data);
    }
  } else if (command === '--enable' || command === 'enable') {
    console.log(`Re-enabling license key: ${target}...`);
    const { status, data } = await api('/v1/admin/licenses/enable', 'POST', { licenseKey: target });
    if (status === 200 && data.enabled) {
      console.log(`✅ License key [${target}] has been RE-ENABLED.`);
    } else {
      console.error(`❌ Failed to enable license:`, data);
    }
  } else {
    console.error(`Unknown option: ${command}. Run with --help for usage.`);
  }
}

main().catch(console.error);
