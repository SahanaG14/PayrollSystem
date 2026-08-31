# Payroll System licensing service

This Worker issues **lifetime** licenses: a record has no expiry date, and each license can be activated only up to its configured number of computers.

## Deploy

1. Create a free Cloudflare account and install Wrangler.
2. From this folder, create a D1 database: `npx wrangler d1 create payroll-licenses`.
3. Copy `wrangler.toml.example` to `wrangler.toml` and insert the database ID returned above.
4. Apply the schema: `npx wrangler d1 execute payroll-licenses --remote --file=schema.sql`.
5. Set an administrator secret (use a long random value): `npx wrangler secret put ADMIN_SECRET`.
6. Deploy: `npx wrangler deploy`.

Set the application launch option to the deployed address, for example:

`-Dpayroll.license.url=https://payroll-license-api.<your-subdomain>.workers.dev`

For a packaged installer, put that JVM option in its launcher configuration before distributing it. Do not allow end users to change it.

## Create a license after a one-time purchase

Generate a long random key (for example `PAY-` followed by 32 random characters) and retain it only long enough to deliver it to the buyer. Then call:

```sh
curl -X POST https://YOUR-WORKER.workers.dev/v1/admin/licenses \
  -H 'Authorization: Bearer YOUR_ADMIN_SECRET' \
  -H 'Content-Type: application/json' \
  -d '{"licenseKey":"PAY-EXAMPLE-KEY","maxSeats":1}'
```

The Worker stores only a SHA-256 hash of the license key. To move a customer to a replacement computer, revoke their activation through `POST /v1/admin/activations/{activationId}/revoke`, then have them activate the new computer. Keep `ADMIN_SECRET` private; never put it in the desktop application.

## Important security note

This is a practical licensing control, not unbreakable DRM. A determined attacker can alter a local Java application. Code signing, obfuscation, signed server responses, and a proper admin dashboard are sensible later additions as sales grow.
