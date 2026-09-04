CREATE TABLE IF NOT EXISTS support_audit_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  action_name TEXT NOT NULL,
  recovery_code_id TEXT,
  support_id TEXT,
  request_ip TEXT,
  details TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
