CREATE TABLE IF NOT EXISTS recovery_codes (
  id TEXT PRIMARY KEY,
  activation_id INTEGER NOT NULL REFERENCES activations(id) ON DELETE CASCADE,
  code_hash TEXT NOT NULL UNIQUE,
  expires_at INTEGER NOT NULL,
  used_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS recovery_codes_active_idx ON recovery_codes(activation_id, expires_at, used_at);
