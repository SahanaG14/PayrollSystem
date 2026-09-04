const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json", "cache-control": "no-store" } });
const hash = async value => Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)))).map(x => x.toString(16).padStart(2, "0")).join("");
const authorized = (request, env) => request.headers.get("authorization") === `Bearer ${env.ADMIN_SECRET}`;
const WINDOW_MS = 60_000, LIMIT = 20;
const encoded = bytes => btoa(String.fromCharCode(...new Uint8Array(bytes))).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
const random = length => encoded(crypto.getRandomValues(new Uint8Array(length)));
async function sign(env, value) { const key=await crypto.subtle.importKey("raw",new TextEncoder().encode(env.RECOVERY_SECRET),{name:"HMAC",hash:"SHA-256"},false,["sign"]);return encoded(await crypto.subtle.sign("HMAC",key,new TextEncoder().encode(value))); }
async function supportAudit(request, env, action, recoveryId, supportId, details) { await env.DB.prepare("INSERT INTO support_audit_log(action_name,recovery_code_id,support_id,request_ip,details) VALUES(?,?,?,?,?)").bind(action,recoveryId || null,supportId || null,request.headers.get("CF-Connecting-IP") || null,details || null).run(); }

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
      return json({ status: "healthy", service: "Yashasvi Accounting Solutions LLP Licensing API" });
    }
    if (request.method === "POST" && (pathname === "/v1/activate" || pathname === "/v1/validate")) {
      if (await limited(request, env)) return json({ valid: false, message: "Too many requests. Try again shortly." }, 429);
      return licensed(request, env, pathname === "/v1/activate");
    }
    if (request.method === "POST" && pathname === "/v1/recovery/verify") {
      if (await limited(request, env)) return json({ valid: false, message: "Too many requests. Try again shortly." }, 429);
      const input=await request.json().catch(()=>({}));
      if (!input.licenseKey || !input.machineId || !input.recoveryCode) return json({valid:false,message:"Invalid recovery request."},400);
      const activation=await env.DB.prepare("SELECT a.id,a.revoked_at FROM activations a JOIN licenses l ON l.id=a.license_id WHERE l.key_hash=? AND l.active=1 AND a.machine_id=?").bind(await hash(String(input.licenseKey).trim()),String(input.machineId)).first();
      const pieces=String(input.recoveryCode).split("."); if(!activation || activation.revoked_at || pieces.length!==3) return json({valid:false,message:"Recovery code is invalid."},403);
      const record=await env.DB.prepare("SELECT * FROM recovery_codes WHERE id=? AND activation_id=? AND code_hash=? AND used_at IS NULL AND expires_at>? ").bind(pieces[0],activation.id,await hash(String(input.recoveryCode)),Date.now()).first();
      if(!record || !env.RECOVERY_SECRET || pieces[2]!==await sign(env,`${pieces[0]}.${pieces[1]}.${record.expires_at}.${input.machineId}`))return json({valid:false,message:"Recovery code is invalid, expired, or has already been used."},403);
      const consumed=await env.DB.prepare("UPDATE recovery_codes SET used_at=CURRENT_TIMESTAMP WHERE id=? AND used_at IS NULL").bind(record.id).run();
      return consumed.meta?.changes===1?json({valid:true}):json({valid:false,message:"Recovery code has already been used."},403);
    }
    if (request.method === "POST" && pathname === "/v1/recovery/check") {
      if (await limited(request, env)) return json({ valid: false, message: "Too many requests. Try again shortly." }, 429);
      const input=await request.json().catch(()=>({})); if(!input.licenseKey || !input.machineId) return json({valid:false,message:"Invalid recovery request."},400);
      const activation=await env.DB.prepare("SELECT a.id,a.revoked_at FROM activations a JOIN licenses l ON l.id=a.license_id WHERE l.key_hash=? AND l.active=1 AND a.machine_id=?").bind(await hash(String(input.licenseKey).trim()),String(input.machineId)).first();
      if(!activation || activation.revoked_at)return json({valid:false,message:"No credential reset is available."},403);
      const pending=await env.DB.prepare("SELECT id FROM recovery_codes WHERE activation_id=? AND used_at IS NULL AND expires_at>? ORDER BY created_at DESC LIMIT 1").bind(activation.id,Date.now()).first();
      if(!pending)return json({valid:false,message:"No credential reset is available."},403);
      const consumed=await env.DB.prepare("UPDATE recovery_codes SET used_at=CURRENT_TIMESTAMP WHERE id=? AND used_at IS NULL").bind(pending.id).run();
      return consumed.meta?.changes===1?json({valid:true}):json({valid:false,message:"Credential reset is no longer available."},403);
    }
    if (!authorized(request, env)) return json({ message: "Unauthorized" }, 401);
    if (request.method === "GET" && pathname === "/v1/admin/licenses") {
      const rows = await env.DB.prepare(`
        SELECT l.id, l.max_seats, l.active, l.expires_at, l.created_at,
               a.id AS activation_id, a.machine_id, a.machine_name, a.activated_at, a.last_seen_at, a.revoked_at
        FROM licenses l
        LEFT JOIN activations a ON a.license_id = l.id
        ORDER BY l.id DESC
      `).all();
      return json({ results: rows.results });
    }
    if (request.method === "POST" && pathname === "/v1/admin/licenses") {
      const input = await request.json().catch(() => ({}));
      const seats = Number(input.maxSeats || 1);
      if (!input.licenseKey || !Number.isInteger(seats) || seats < 1 || seats > 10000) return json({ message: "licenseKey and maxSeats are required." }, 400);
      try { await env.DB.prepare("INSERT INTO licenses (key_hash, max_seats) VALUES (?, ?)").bind(await hash(input.licenseKey.trim()), seats).run(); return json({ created: true, maxSeats: seats }, 201); }
      catch { return json({ message: "That license key already exists." }, 409); }
    }
    if (request.method === "POST" && pathname === "/v1/admin/licenses/disable") {
      const input = await request.json().catch(() => ({}));
      if (!input.licenseKey) return json({ message: "licenseKey is required." }, 400);
      const keyHash = await hash(input.licenseKey.trim());
      const res = await env.DB.prepare("UPDATE licenses SET active = 0 WHERE key_hash = ?").bind(keyHash).run();
      return json({ disabled: res.meta && res.meta.changes > 0 });
    }
    if (request.method === "POST" && pathname === "/v1/admin/licenses/enable") {
      const input = await request.json().catch(() => ({}));
      if (!input.licenseKey) return json({ message: "licenseKey is required." }, 400);
      const keyHash = await hash(input.licenseKey.trim());
      const res = await env.DB.prepare("UPDATE licenses SET active = 1 WHERE key_hash = ?").bind(keyHash).run();
      return json({ enabled: res.meta && res.meta.changes > 0 });
    }
    if (request.method === "POST" && pathname === "/v1/admin/licenses/revoke-activations") {
      const input = await request.json().catch(() => ({}));
      if (!input.licenseKey) return json({ message: "licenseKey is required." }, 400);
      const keyHash = await hash(input.licenseKey.trim());
      const license = await env.DB.prepare("SELECT id FROM licenses WHERE key_hash = ?").bind(keyHash).first();
      if (!license) return json({ message: "License not found." }, 404);
      const res = await env.DB.prepare("UPDATE activations SET revoked_at = CURRENT_TIMESTAMP WHERE license_id = ? AND revoked_at IS NULL").bind(license.id).run();
      return json({ revokedCount: res.meta ? res.meta.changes : 0 });
    }
    if (request.method === "POST" && pathname === "/v1/admin/recovery-codes") {
      const input=await request.json().catch(()=>({})), minutes=Number(input.expiresMinutes || 15);
      const support=String(input.supportId || "").toUpperCase(), machine=String(input.machineId || "");
      if((machine.length!==64 && !/^YASL-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$/.test(support)) || !Number.isInteger(minutes) || minutes<5 || minutes>60 || !env.RECOVERY_SECRET)return json({message:"supportId and an expiry from 5 to 60 minutes are required."},400);
      const activation=machine.length===64 ? await env.DB.prepare("SELECT id,machine_id FROM activations WHERE machine_id=? AND revoked_at IS NULL ORDER BY id DESC LIMIT 1").bind(machine).first() : await env.DB.prepare("SELECT id,machine_id FROM activations WHERE ('YASL-' || upper(substr(machine_id,1,4)) || '-' || upper(substr(machine_id,5,4)) || '-' || upper(substr(machine_id,9,4)))=? AND revoked_at IS NULL ORDER BY id DESC LIMIT 1").bind(support).first();
      if(!activation)return json({message:"No active installation matches this Support ID."},404);
      const id=random(12), token=random(24), expiresAt=Date.now()+minutes*60_000, recoveryCode=`${id}.${token}.${await sign(env,`${id}.${token}.${expiresAt}.${activation.machine_id}`)}`;
      await env.DB.prepare("INSERT INTO recovery_codes(id,activation_id,code_hash,expires_at) VALUES(?,?,?,?)").bind(id,activation.id,await hash(recoveryCode),expiresAt).run();
      await supportAudit(request,env,"RECOVERY CODE GENERATED",id,support || `YASL-${activation.machine_id.slice(0,4).toUpperCase()}-${activation.machine_id.slice(4,8).toUpperCase()}-${activation.machine_id.slice(8,12).toUpperCase()}`,`Expiry: ${new Date(expiresAt).toISOString()}`);
      return json({recoveryId:id,recoveryCode,expiresAt},201);
    }
    const cancelRecovery = pathname.match(/^\/v1\/admin\/recovery-codes\/([A-Za-z0-9_-]+)\/revoke$/);
    if (request.method === "POST" && cancelRecovery) { const code=await env.DB.prepare("SELECT id,activation_id FROM recovery_codes WHERE id=? AND used_at IS NULL").bind(cancelRecovery[1]).first(); if(!code)return json({message:"Active recovery code was not found."},404); await env.DB.prepare("UPDATE recovery_codes SET used_at=CURRENT_TIMESTAMP WHERE id=? AND used_at IS NULL").bind(code.id).run(); await supportAudit(request,env,"RECOVERY CODE REVOKED",code.id,null,"Revoked by YASL support"); return json({revoked:true}); }
    const revoke = pathname.match(/^\/v1\/admin\/activations\/(\d+)\/revoke$/);
    if (request.method === "POST" && revoke) { await env.DB.prepare("UPDATE activations SET revoked_at = CURRENT_TIMESTAMP WHERE id = ?").bind(revoke[1]).run(); return json({ revoked: true }); }
    return json({ message: "Not found" }, 404);
  }
};
