# Production security checklist

Before distributing an installer, complete these account-bound actions:

1. Create the Cloudflare D1 database, apply `license-server/schema.sql`, deploy the Worker, and set a random `ADMIN_SECRET` with `wrangler secret put ADMIN_SECRET`.
2. Replace the launch-time license URL with the HTTPS Worker URL in the packaged launcher. Do not expose the administrator secret in the application.
3. Obtain a code-signing certificate for each installer platform and sign the final installer and executable.
4. Adopt SQLCipher (or a managed encrypted datastore) before handling production payroll data. The stock SQLite JDBC driver does not encrypt `payroll.db`.
5. Store the database key only in the operating-system credential store, not in source code, Preferences, or a configuration file.
6. Encrypt backups with an operator-supplied password and test a restore using a copy of production-like data.
7. Restrict the backup destination to a protected location and test restore failure for corrupted, oversized, and malicious archives.
8. Upgrade dependencies, run dependency scanning, and run the application test suite on a machine with JDK 17 and Maven installed.
9. Require unique operating-system accounts for people who use Payroll System. A desktop app cannot protect data from a person who controls the same OS account and application files.

Do not release while a default, development, or user-overridable license endpoint is configured.
