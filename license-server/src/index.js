const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json", "cache-control": "no-store" } });
const hash = async value => Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)))).map(x => x.toString(16).padStart(2, "0")).join("");
const authorized = (request, env) => request.headers.get("authorization") === `Bearer ${env.ADMIN_SECRET}`;
const WINDOW_MS = 60_000, LIMIT = 20;

async function limited(request, env) {
  const now = Date.now(), bucket = `${request.headers.get("CF-Connecting-IP") || "unknown"}:${Math.floor(now / WINDOW_MS)}`;
  await env.DB.prepare("INSERT INTO request_limits(bucket, request_count, expires_at) VALUES (?, 1, ?) ON CONFLICT(bucket) DO UPDATE SET request_count = request_count + 1").bind(bucket, now + WINDOW_MS).run();
  const count = await env.DB.prepare("SELECT request_count FROM request_limits WHERE bucket = ?").bind(bucket).first();
  if (Math.random() < 0.01) await env.DB.prepare("DELETE FROM request_limits WHERE expires_at < ?").bind(now).run();
  return count.request_count > LIMIT;
}

async function licensed(request, env, createActivation) {
  const input = await request.json().catch(() => ({}));
  if (!input.licenseKey || !input.machineId || input.licenseKey.length > 200 || input.machineId.length !== 64) return json({ valid: false, message: "Invalid activation request." }, 400);
  const keyHash = await hash(input.licenseKey.trim());
  const license = await env.DB.prepare("SELECT * FROM licenses WHERE key_hash = ?").bind(keyHash).first();
  if (!license || !license.active) return json({ valid: false, message: "License key is invalid or disabled." }, 403);
  if (license.expires_at && Date.parse(license.expires_at) < Date.now()) return json({ valid: false, message: "License has expired." }, 403);
  const existing = await env.DB.prepare("SELECT id, revoked_at FROM activations WHERE license_id = ? AND machine_id = ?").bind(license.id, input.machineId).first();
  if (existing) {
    if (!existing.revoked_at) {
      await env.DB.prepare("UPDATE activations SET last_seen_at = CURRENT_TIMESTAMP WHERE id = ?").bind(existing.id).run();
      return json({ valid: true, maxSeats: license.max_seats });
    }
    if (!createActivation) return json({ valid: false, message: "This computer activation was revoked." }, 403);
    const reactivated = await env.DB.prepare("UPDATE activations SET revoked_at = NULL, activated_at = CURRENT_TIMESTAMP, last_seen_at = CURRENT_TIMESTAMP, machine_name = ? WHERE id = ? AND (SELECT COUNT(*) FROM activations WHERE license_id = ? AND revoked_at IS NULL) < ?").bind(String(input.machineName || "Unknown computer").slice(0, 100), existing.id, license.id, license.max_seats).run();
    if (reactivated.meta && reactivated.meta.changes === 1) return json({ valid: true, maxSeats: license.max_seats });
    return json({ valid: false, message: "This license has reached its computer limit." }, 403);
  }
  if (!createActivation) return json({ valid: false, message: "This computer has not been activated." }, 403);
  const created = await env.DB.prepare("INSERT OR IGNORE INTO activations (license_id, machine_id, machine_name) SELECT ?, ?, ? WHERE (SELECT COUNT(*) FROM activations WHERE license_id = ? AND revoked_at IS NULL) < ?").bind(license.id, input.machineId, String(input.machineName || "Unknown computer").slice(0, 100), license.id, license.max_seats).run();
  if (created.meta && created.meta.changes === 1) return json({ valid: true, maxSeats: license.max_seats });
  const nowActive = await env.DB.prepare("SELECT id FROM activations WHERE license_id = ? AND machine_id = ? AND revoked_at IS NULL").bind(license.id, input.machineId).first();
  return nowActive ? json({ valid: true, maxSeats: license.max_seats }) : json({ valid: false, message: "This license has reached its computer limit." }, 403);
}

export default {
  async fetch(request, env) {
    const { pathname } = new URL(request.url);
    if (request.method === "GET" && (pathname === "/" || pathname === "/health")) {
      return json({ status: "healthy", service: "Payroll System Licensing API" });
    }
    if (request.method === "POST" && (pathname === "/v1/activate" || pathname === "/v1/validate")) {
      if (await limited(request, env)) return json({ valid: false, message: "Too many requests. Try again shortly." }, 429);
      return licensed(request, env, pathname === "/v1/activate");
    }
    if (!authorized(request, env)) return json({ message: "Unauthorized" }, 401);
    if (request.method === "POST" && pathname === "/v1/admin/licenses") {
      const input = await request.json().catch(() => ({}));
      const seats = Number(input.maxSeats || 1);
      if (!input.licenseKey || !Number.isInteger(seats) || seats < 1 || seats > 10000) return json({ message: "licenseKey and maxSeats are required." }, 400);
      try { await env.DB.prepare("INSERT INTO licenses (key_hash, max_seats) VALUES (?, ?)").bind(await hash(input.licenseKey.trim()), seats).run(); return json({ created: true, maxSeats: seats }, 201); }
      catch { return json({ message: "That license key already exists." }, 409); }
    }
    const revoke = pathname.match(/^\/v1\/admin\/activations\/(\d+)\/revoke$/);
    if (request.method === "POST" && revoke) { await env.DB.prepare("UPDATE activations SET revoked_at = CURRENT_TIMESTAMP WHERE id = ?").bind(revoke[1]).run(); return json({ revoked: true }); }
    return json({ message: "Not found" }, 404);
  }
};
