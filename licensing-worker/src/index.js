const json = (value, status = 200) => new Response(JSON.stringify(value), {status, headers: {"content-type":"application/json"}});
const hash = async value => { const data = new TextEncoder().encode(value); const bytes = await crypto.subtle.digest("SHA-256", data); return [...new Uint8Array(bytes)].map(x => x.toString(16).padStart(2,"0")).join(""); };
const id = () => crypto.randomUUID();
const authorized = request => request.headers.get("authorization") === `Bearer ${request.env.ADMIN_SECRET}`;

async function activate(request, env) {
  const {licenseKey, deviceId} = await request.json().catch(() => ({}));
  if (!licenseKey || !deviceId) return json({error:"licenseKey and deviceId are required"}, 400);
  const license = await env.DB.prepare("SELECT id,max_seats FROM licenses WHERE key_hash=?").bind(await hash(licenseKey)).first();
  if (!license) return json({allowed:false,error:"Invalid license key"}, 403);
  const deviceHash = await hash(deviceId);
  const existing = await env.DB.prepare("SELECT id,revoked_at FROM activations WHERE license_id=? AND device_hash=?").bind(license.id, deviceHash).first();
  if (existing && !existing.revoked_at) return json({allowed:true,activationId:existing.id});
  const count = await env.DB.prepare("SELECT COUNT(*) count FROM activations WHERE license_id=? AND revoked_at IS NULL").bind(license.id).first();
  if (count.count >= license.max_seats) return json({allowed:false,error:"Activation limit reached"}, 409);
  if (existing) await env.DB.prepare("UPDATE activations SET revoked_at=NULL,activated_at=CURRENT_TIMESTAMP WHERE id=?").bind(existing.id).run();
  else await env.DB.prepare("INSERT INTO activations(id,license_id,device_hash) VALUES(?,?,?)").bind(id(),license.id,deviceHash).run();
  return json({allowed:true});
}
async function validate(request, env) {
  const {licenseKey, deviceId} = await request.json().catch(() => ({}));
  if (!licenseKey || !deviceId) return json({allowed:false}, 400);
  const license = await env.DB.prepare("SELECT id FROM licenses WHERE key_hash=?").bind(await hash(licenseKey)).first();
  if (!license) return json({allowed:false}, 403);
  const activation = await env.DB.prepare("SELECT id FROM activations WHERE license_id=? AND device_hash=? AND revoked_at IS NULL").bind(license.id,await hash(deviceId)).first();
  return json({allowed:!!activation,activationId:activation?.id || null}, activation ? 200 : 403);
}
export default { async fetch(request, env) {
  const url = new URL(request.url), path = url.pathname;
  if (request.method === "POST" && path === "/v1/activate") return activate(request,env);
  if (request.method === "POST" && path === "/v1/validate") return validate(request,env);
  if (!authorized(request)) return json({error:"Unauthorized"},401);
  if (request.method === "POST" && path === "/v1/admin/licenses") { const {licenseKey,maxSeats} = await request.json().catch(()=>({})); if(!licenseKey || !Number.isInteger(maxSeats) || maxSeats < 1)return json({error:"licenseKey and positive integer maxSeats required"},400); await env.DB.prepare("INSERT INTO licenses(id,key_hash,max_seats) VALUES(?,?,?)").bind(id(),await hash(licenseKey),maxSeats).run(); return json({created:true},201); }
  const match = path.match(/^\/v1\/admin\/activations\/([^/]+)\/revoke$/);
  if (request.method === "POST" && match) { await env.DB.prepare("UPDATE activations SET revoked_at=CURRENT_TIMESTAMP WHERE id=? AND revoked_at IS NULL").bind(match[1]).run(); return json({revoked:true}); }
  return json({error:"Not found"},404);
} };
