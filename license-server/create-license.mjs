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

function generateKey() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // readable chars excluding easily confused ones
  const part = (len) => Array.from({ length: len }, () => chars[Math.floor(Math.random() * chars.length)]).join("");
  return `PAY-${part(4)}-${part(4)}-${part(4)}-${part(4)}`;
}

async function createLicense(key, seats = 1) {
  if (!adminSecret) {
    console.error("ADMIN_SECRET is not configured.");
    process.exit(1);
  }
  const res = await fetch(`${workerUrl}/v1/admin/licenses`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${adminSecret}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ licenseKey: key, maxSeats: seats })
  });
  const data = await res.json();
  if (res.ok) {
    console.log(`\n🎉 License Key Generated & Registered:`);
    console.log(`   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
    console.log(`   Key       : ${key}`);
    console.log(`   Max Seats : ${seats}`);
    console.log(`   Server    : ${workerUrl}`);
    console.log(`   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`);
  } else {
    console.error(`\n❌ Failed to create license:`, data);
  }
}

const args = process.argv.slice(2);
const seats = parseInt(args[0], 10) || 1;
const customKey = args[1];
const key = customKey || generateKey();

createLicense(key, seats);
